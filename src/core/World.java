package core;

import tileengine.TETile;
import tileengine.Tileset;
import tileengine.AvatarTileset;

import java.awt.*;
import java.util.*;
import java.util.List;

public class World {

    // build your own world!
    static int WIDTH = 80;
    static int HEIGHT = 45;
    final private static TETile UNUSED = Tileset.NOTHING;
    final static TETile FLOOR = Tileset.FLOOR;
    final static TETile WALL = Tileset.WALL;
    final private static long SEEDDefault = 87654L;
    final private static TETile CHASER = Tileset.CHASER;
    private int NUMBER_OF_CONSUMABLES = 10;

    private int avatarX, avatarY;
    private int chaserX, chaserY;
    private boolean chaserIsDead = false;
    private boolean isShowPath;
    private List<Point> pathToAvatar;

    private long seed;
    private Random random;
    private TETile[][] map;
    private ArrayList<Room> rooms;
    private ArrayList<Hallway> hallways;
    private Set<Point> usedSpaces;
    private Player player;

    private int doorX, doorY;

    private List<Consumable> consumables; // Class-level variable

    private Set<Point> consumablePositions; // To store positions of consumables

    private EventDispatcher eventDispatcher;

    private Map<Point, ObstacleType> obstacles = new HashMap<>();

    private char lastDirection;

    private boolean isDarkMode = false;
    private int visionRadius = 5; // Default vision radius
    private TETile[][] visibleMap; // For storing what player can actually see

    private static final long FLASH_INTERVAL = 1000; // 4 seconds in milliseconds

    // Field to track whether the eerie sound is currently playing
    private boolean isEerieSoundPlaying = false;
    // Field to track if the chaser sound is currently playing
    private boolean isChaserSoundPlaying = false;

    private long lastProximityCheck = 0;

    // Optimize path finding by caching results
    private Map<String, ArrayList<Point>> pathCache = new HashMap<>();
    private static final long PATH_CACHE_DURATION = 500; // 500ms cache duration
    private long lastPathCalculation = 0;

    // Add a map to store tiles that are underneath the chaser
    private TETile tileUnderChaser = FLOOR; // Initialize to FLOOR

    private long lastPathFlashTime = 0;
    private static final long PATH_FLASH_INTERVAL = 500; // 0.5 seconds
    private boolean showPathThisFrame = false;

    private Set<Point> torchPositions = new HashSet<>();

    public World() {
        this(null, SEEDDefault);
    }

    public World(Player player, Long seed) {
        this(player, seed, 10, 10);
    }

    public World(Player player, long seed, int numConsumables, int numObstacles) {
        this.player = player;
        this.seed = seed;
        this.lastDirection = 's';
        rooms = new ArrayList<>();
        hallways = new ArrayList<>();
        random = new Random(seed);
        usedSpaces = new HashSet<>();
        this.consumables = new ArrayList<>();
        this.consumablePositions = new HashSet<>();
        this.eventDispatcher = new EventDispatcher();
        map = new TETile[WIDTH][HEIGHT];
        initializeWorldWithTiles();
        placeAvatar();
        placeChaser();
        // Only place door during initial world creation, not when loading
        if (numConsumables > 0 || numObstacles > 0) { // If these are 0, we're loading a saved game
            placeDoor();
            populateConsumables(numConsumables);
            populateObstacles(numObstacles);
        }
    }

    private void populateConsumables(int numConsumables) {
        NUMBER_OF_CONSUMABLES = numConsumables;
        consumables.add(new Consumable("Smiley Face", 10, Tileset.SMILEY_FACE_green_body_circle));
        consumables.add(new Consumable("Normal Face", 5, Tileset.SMILEY_FACE_green_body_rhombus));

        // Create zones for better distribution
        List<Zone> zones = createZones();

        // Calculate items per zone
        int itemsPerZone = Math.max(1, numConsumables / zones.size());
        int remainingItems = numConsumables % zones.size();

        Random rand = new Random(seed);

        // Distribute items across zones
        for (Zone zone : zones) {
            int zoneItems = itemsPerZone + (remainingItems > 0 ? 1 : 0);
            remainingItems--;

            List<Point> zonePositions = getAvailablePositionsInZone(zone);

            // Apply weighted distribution within zone
            for (int i = 0; i < zoneItems && !zonePositions.isEmpty(); i++) {
                Point position = selectPositionWithWeights(zonePositions, zone);
                if (position != null) {
                    // Select consumable type based on position value
                    Consumable consumable = selectConsumableByValue(position, zone);
                    map[position.x][position.y] = consumable.getTile();
                    consumablePositions.add(position);
                    zonePositions.remove(position);
                }
            }
        }
    }

    private void populateObstacles(int numObstacles) {
        List<Zone> zones = createZones();
        Random rand = new Random(seed);

        // Calculate base obstacles per zone
        int obstaclesPerZone = Math.max(1, numObstacles / zones.size());
        int remainingObstacles = numObstacles % zones.size();

        // Ensure minimum dark rooms (20% of total)
        int totalDarkRooms = Math.max(1, numObstacles / 5);
        int darkRoomsPerZone = Math.max(1, totalDarkRooms / zones.size());

        for (Zone zone : zones) {
            int zoneObstacles = obstaclesPerZone + (remainingObstacles > 0 ? 1 : 0);
            remainingObstacles--;

            List<Point> zonePositions = getAvailablePositionsInZone(zone);

            // Place dark rooms first with proper spacing
            for (int i = 0; i < darkRoomsPerZone && !zonePositions.isEmpty(); i++) {
                Point position = selectPositionForDarkRoom(zonePositions, zone);
                if (position != null) {
                    obstacles.put(position, ObstacleType.DARK_MODE);
                    map[position.x][position.y] = ObstacleType.DARK_MODE.getTile();
                    removeNearbyPositions(zonePositions, position, 5); // Ensure spacing
                }
            }

            // Place other obstacles with weighted distribution
            int remainingZoneObstacles = zoneObstacles - darkRoomsPerZone;
            for (int i = 0; i < remainingZoneObstacles && !zonePositions.isEmpty(); i++) {
                Point position = selectPositionWithWeights(zonePositions, zone);
                if (position != null) {
                    ObstacleType obstacle = selectObstacleByLocation(position, zone);
                    obstacles.put(position, obstacle);
                    map[position.x][position.y] = obstacle.getTile();
                    removeNearbyPositions(zonePositions, position, 3); // Smaller spacing for regular obstacles
                }
            }
        }
    }

    // Helper class for world zones
    private class Zone {
        int startX, startY, endX, endY;
        double difficulty; // 0.0 to 1.0, based on distance from start

        Zone(int startX, int startY, int endX, int endY) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.difficulty = calculateZoneDifficulty();
        }

        private double calculateZoneDifficulty() {
            // Calculate center point of zone
            int centerX = (startX + endX) / 2;
            int centerY = (startY + endY) / 2;

            // Calculate distance from avatar start position
            double distFromStart = Math.sqrt(
                    Math.pow(centerX - avatarX, 2) +
                            Math.pow(centerY - avatarY, 2));

            // Normalize distance to 0-1 range
            return Math.min(1.0, distFromStart / (Math.sqrt(WIDTH * WIDTH + HEIGHT * HEIGHT)));
        }
    }

    private List<Zone> createZones() {
        List<Zone> zones = new ArrayList<>();
        int zoneSize = 10; // Adjust based on world size

        for (int x = 0; x < WIDTH; x += zoneSize) {
            for (int y = 0; y < HEIGHT; y += zoneSize) {
                int endX = Math.min(x + zoneSize, WIDTH);
                int endY = Math.min(y + zoneSize, HEIGHT);
                zones.add(new Zone(x, y, endX, endY));
            }
        }

        return zones;
    }

    private List<Point> getAvailablePositionsInZone(Zone zone) {
        List<Point> positions = new ArrayList<>();

        for (int x = zone.startX; x < zone.endX; x++) {
            for (int y = zone.startY; y < zone.endY; y++) {
                if (map[x][y] == FLOOR &&
                        (x != avatarX || y != avatarY) &&
                        (x != chaserX || y != chaserY) &&
                        (x != doorX || y != doorY)) {
                    positions.add(new Point(x, y));
                }
            }
        }

        return positions;
    }

    private Point selectPositionWithWeights(List<Point> positions, Zone zone) {
        if (positions.isEmpty())
            return null;

        // Calculate weights for each position
        double[] weights = new double[positions.size()];
        double totalWeight = 0;

        for (int i = 0; i < positions.size(); i++) {
            Point p = positions.get(i);
            double weight = calculatePositionWeight(p, zone);
            weights[i] = weight;
            totalWeight += weight;
        }

        // Select position based on weights
        double selection = random.nextDouble() * totalWeight;
        double currentSum = 0;

        for (int i = 0; i < weights.length; i++) {
            currentSum += weights[i];
            if (currentSum >= selection) {
                return positions.get(i);
            }
        }

        return positions.get(0);
    }

    private double calculatePositionWeight(Point p, Zone zone) {
        // Base weight
        double weight = 1.0;

        // Factor in distance from walls (prefer positions away from walls)
        weight *= getWallDistanceFactor(p);

        // Factor in distance from other items
        weight *= getItemSpacingFactor(p);

        // Factor in zone difficulty
        weight *= (1.0 + zone.difficulty);

        // Factor in path accessibility
        weight *= getPathAccessibilityFactor(p);

        return weight;
    }

    private double getWallDistanceFactor(Point p) {
        int wallCount = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int newX = p.x + dx;
                int newY = p.y + dy;
                if (newX >= 0 && newX < WIDTH && newY >= 0 && newY < HEIGHT) {
                    if (map[newX][newY] == WALL)
                        wallCount++;
                }
            }
        }
        return 1.0 - (wallCount / 8.0);
    }

    private double getItemSpacingFactor(Point p) {
        double minDistance = Double.MAX_VALUE;

        // Check distance to consumables
        for (Point other : consumablePositions) {
            double dist = Math.sqrt(Math.pow(p.x - other.x, 2) + Math.pow(p.y - other.y, 2));
            minDistance = Math.min(minDistance, dist);
        }

        // Check distance to obstacles
        for (Point other : obstacles.keySet()) {
            double dist = Math.sqrt(Math.pow(p.x - other.x, 2) + Math.pow(p.y - other.y, 2));
            minDistance = Math.min(minDistance, dist);
        }

        return Math.min(1.0, minDistance / 5.0);
    }

    private double getPathAccessibilityFactor(Point p) {
        // Calculate accessibility based on number of accessible neighbors
        int accessibleNeighbors = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0)
                    continue;
                int newX = p.x + dx;
                int newY = p.y + dy;
                if (newX >= 0 && newX < WIDTH && newY >= 0 && newY < HEIGHT) {
                    if (map[newX][newY] == FLOOR)
                        accessibleNeighbors++;
                }
            }
        }
        return accessibleNeighbors / 8.0;
    }

    private Consumable selectConsumableByValue(Point position, Zone zone) {
        // Higher value consumables in more difficult zones
        if (random.nextDouble() < zone.difficulty) {
            return consumables.get(0); // Smiley Face (higher value)
        } else {
            return consumables.get(1); // Normal Face (lower value)
        }
    }

    private ObstacleType selectObstacleByLocation(Point position, Zone zone) {
        double roll = random.nextDouble();

        // More dangerous obstacles in difficult zones
        if (zone.difficulty > 0.7) {
            if (roll < 0.4)
                return ObstacleType.SPIKES;
            else if (roll < 0.7)
                return ObstacleType.TELEPORTER;
            else
                return ObstacleType.ICE;
        } else if (zone.difficulty > 0.3) {
            if (roll < 0.3)
                return ObstacleType.SPIKES;
            else if (roll < 0.6)
                return ObstacleType.TELEPORTER;
            else
                return ObstacleType.ICE;
        } else {
            if (roll < 0.2)
                return ObstacleType.SPIKES;
            else if (roll < 0.5)
                return ObstacleType.TELEPORTER;
            else
                return ObstacleType.ICE;
        }
    }

    private Point selectPositionForDarkRoom(List<Point> positions, Zone zone) {
        // Find position with good spacing from other dark rooms
        Point bestPosition = null;
        double bestScore = -1;

        for (Point p : positions) {
            double score = 1.0;

            // Check distance from other dark rooms
            for (Map.Entry<Point, ObstacleType> entry : obstacles.entrySet()) {
                if (entry.getValue() == ObstacleType.DARK_MODE) {
                    double distance = Math.sqrt(
                            Math.pow(p.x - entry.getKey().x, 2) +
                                    Math.pow(p.y - entry.getKey().y, 2));
                    score *= Math.min(1.0, distance / 10.0);
                }
            }

            // Factor in zone difficulty
            score *= (1.0 + zone.difficulty);

            if (score > bestScore) {
                bestScore = score;
                bestPosition = p;
            }
        }

        return bestPosition;
    }

    private void removeNearbyPositions(List<Point> positions, Point center, int radius) {
        positions.removeIf(p -> Math.sqrt(Math.pow(p.x - center.x, 2) + Math.pow(p.y - center.y, 2)) <= radius);
    }

    public void placeAvatar() {
        List<Point> availablePositions = new ArrayList<>(usedSpaces);
        availablePositions.removeIf(point -> map[point.x][point.y] != FLOOR);

        if (!availablePositions.isEmpty()) {
            Random rand = new Random();
            Point randomPosition = availablePositions.get(rand.nextInt(availablePositions.size()));
            avatarX = randomPosition.x;
            avatarY = randomPosition.y;
            updateAvatarTile(); // Use this instead of setting a static tile
        }
    }

    private void placeChaser() {
        // Create a list of available positions from usedSpaces
        List<Point> availablePositions = new ArrayList<>(usedSpaces);

        // Filter available positions to only include floor tiles and exclude the
        // avatar's position
        availablePositions
                .removeIf(point -> map[point.x][point.y] != FLOOR || (point.x == avatarX && point.y == avatarY));

        // Find the position that is furthest from the avatar
        Point furthestPosition = null;
        double maxDistance = -1;

        for (Point point : availablePositions) {
            double distance = calculateDistance(point, new Point(avatarX, avatarY));
            if (distance > maxDistance) {
                maxDistance = distance;
                furthestPosition = point;
            }
        }

        // Place the chaser at the furthest position if available
        if (furthestPosition != null) {
            chaserX = furthestPosition.x;
            chaserY = furthestPosition.y;
            map[chaserX][chaserY] = CHASER; // Place the chaser on the map
        }
    }

    private double calculateDistance(Point p1, Point p2) {
        // Calculate Manhattan distance
        return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
    }

    private void placeDoor() {
        List<Point> availablePositions = new ArrayList<>(usedSpaces); // Create a list from usedSpaces

        // Filter available positions to only include wall tiles
        availablePositions.removeIf(point -> map[point.x][point.y] != WALL);

        // Filter available positions to only include wall tiles that are adjacent to a
        // floor or avatar
        // and also adjacent to a nothing tile
        availablePositions.removeIf(point -> {
            boolean adjacentToFloorOrAvatar = false;
            boolean adjacentToNothing = false;

            for (Point neighbor : getAdjacentPoints(point.x, point.y)) {
                if (map[neighbor.x][neighbor.y] == FLOOR || map[neighbor.x][neighbor.y] == map[avatarX][avatarY]) {
                    adjacentToFloorOrAvatar = true;
                }
                if (map[neighbor.x][neighbor.y] == UNUSED) {
                    adjacentToNothing = true;
                }
            }
            // Return true if it does not meet the criteria
            return !(adjacentToFloorOrAvatar && adjacentToNothing);
        });

        // Randomly select one of the available positions for the door
        if (!availablePositions.isEmpty()) {
            Random rand = new Random();
            Point randomPosition = availablePositions.get(rand.nextInt(availablePositions.size()));
            doorX = randomPosition.x;
            doorY = randomPosition.y;
            map[doorX][doorY] = Tileset.LOCKED_DOOR; // Place the door on the map
        }
    }

    private List<Point> getAdjacentPoints(int x, int y) {
        List<Point> neighbors = new ArrayList<>();
        neighbors.add(new Point(x - 1, y)); // Left
        neighbors.add(new Point(x + 1, y)); // Right
        neighbors.add(new Point(x, y - 1)); // Down
        neighbors.add(new Point(x, y + 1)); // Up
        return neighbors;
    }

    // Centralized method to check for chaser-avatar collision and handle the
    // outcome
    boolean handleChaserCollision() {
        if (chaserX == avatarX && chaserY == avatarY && !player.isInvisible()) {
            AudioManager.getInstance().stopSound("chaser");
            eventDispatcher.dispatch(new Event(Event.EventType.GAME_OVER, "The chaser caught you!"));
            chaserIsDead = true;
            return true;
        }
        return false;
    }

    public void moveChaser() {
        if (player.isInvisible()) {
            // In search mode: perform a random walk using adjacent tiles.
            List<Point> neighbors = new ArrayList<>();
            int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
            for (int[] d : directions) {
                int nx = chaserX + d[0];
                int ny = chaserY + d[1];
                if (isWalkable(nx, ny)) {
                    neighbors.add(new Point(nx, ny));
                }
            }
            if (!neighbors.isEmpty()) {
                Point next = neighbors.get(random.nextInt(neighbors.size()));
                setChaserToNewPosition(next.x, next.y);
            }
            checkChaserProximity();
        } else {
            // Normal chasing mode.
            pathToAvatar = findPath(new Point(chaserX, chaserY), new Point(avatarX, avatarY));
            if (pathToAvatar != null && !pathToAvatar.isEmpty()) {
                Point next = pathToAvatar.getFirst();
                setChaserToNewPosition(next.x, next.y);
                checkChaserProximity();
            }
        }
    }

    public void setChaserToNewPosition(int x, int y) {
        // Store the tile at the new position before we move there
        TETile newPosTile = map[x][y];

        // Restore the tile that was under the chaser at the old position
        map[chaserX][chaserY] = tileUnderChaser;

        // Update chaser position
        chaserX = x;
        chaserY = y;

        // Remember what tile is at the new position
        tileUnderChaser = newPosTile;

        // Place chaser at new position
        map[chaserX][chaserY] = CHASER;
    }

    public void togglePathDisplay() {
        isShowPath = !isShowPath;
    }

    public boolean moveAvatar(char direction) {
        lastDirection = direction;
        int newX = avatarX;
        int newY = avatarY;

        // Quick bounds and wall check first
        switch (Character.toLowerCase(direction)) {
            case 'w' -> newY += 1;
            case 's' -> newY -= 1;
            case 'a' -> newX -= 1;
            case 'd' -> newX += 1;
        }

        // Early exit if movement is blocked
        if (newX < 0 || newX >= WIDTH || newY < 0 || newY >= HEIGHT || map[newX][newY] == WALL) {
            return false;
        }

        TETile tileAtNewPosition = map[newX][newY];

        // Cache obstacle at new position
        Point newPos = new Point(newX, newY);
        ObstacleType obstacle = obstacles.get(newPos);

        // Move avatar first for responsive feel
        setAvatarToNewPosition(newX, newY);

        // Handle special tiles after movement
        if (obstacle != null) {
            handleObstacle(obstacle, newPos);
            obstacles.remove(newPos);
        } else if (consumablePositions.contains(newPos)) {
            handleConsumable(newPos, tileAtNewPosition);
        }

        if (tileAtNewPosition == Tileset.TORCH) {
            pickupTorch();
        }

        // Check if reached door
        if (newX == doorX && newY == doorY) {
            map[doorX][doorY] = Tileset.UNLOCKED_DOOR;
        }

        // Perform proximity checks less frequently
        if (System.currentTimeMillis() - lastProximityCheck > 100) {
            checkDarkModeProximity();
            checkChaserProximity();
            lastProximityCheck = System.currentTimeMillis();
        }

        return true;
    }

    public void setAvatarToNewPosition(int newX, int newY) {
        // Store previous position
        int oldX = avatarX;
        int oldY = avatarY;

        // Update position
        avatarX = newX;
        avatarY = newY;

        // Reset old position to floor
        map[oldX][oldY] = FLOOR;

        // Update avatar tile at new position based on player's choice and direction
        updateAvatarTile();
    }

    private void initializeWorldWithTiles() {
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                map[i][j] = UNUSED;
            }
        }
        this.buildWorld();
    }

    public void buildWorld() {
        generateRoom();
        Collections.sort(rooms);
        connectRoomsWithMST();
    }

    private void connectRoomsWithMST() {
        if (rooms.isEmpty()) {
            return;
        }
        PriorityQueue<Edge> pq = new PriorityQueue<>(Comparator.comparingDouble(Edge::getDistance));
        Set<Room> inMST = new HashSet<>();
        Room startRoom = rooms.getFirst();
        inMST.add(startRoom);

        for (Room room : rooms) {
            if (room != startRoom) {
                pq.add(new Edge(startRoom, room, startRoom.calculateDistance(room)));
            }
        }

        while ((inMST.size() < rooms.size())) {
            Edge minEdge = pq.poll();
            assert minEdge != null;
            if (inMST.contains(minEdge.getRoom1()) && inMST.contains(minEdge.getRoom2())) {
                continue;
            }
            Room newRoom = inMST.contains(minEdge.getRoom1()) ? minEdge.getRoom2() : minEdge.getRoom1();
            inMST.add(newRoom);

            connectRooms(minEdge.getRoom1(), minEdge.getRoom2());

            for (Room room : rooms) {
                if (!inMST.contains(room)) {
                    pq.add(new Edge(newRoom, room, newRoom.calculateDistance(room)));
                }
            }
        }
    }

    public void generateRoom() {
        int difficulty = player.calculateDifficulty(); // Get difficulty based on player points
        int minRooms = 1 + difficulty;
        int maxRooms = 5 + (difficulty * 3);
        int roomNums = random.nextInt(maxRooms - minRooms + 1) + minRooms;
        // int roomNums = 3;

        // Generate rooms within the grid boundaries
        while (rooms.size() < roomNums) {
            int width = random.nextInt(10) + 5;
            int height = random.nextInt(7) + 3;
            int x = random.nextInt(WIDTH - width - 2) + 1;
            int y = random.nextInt(HEIGHT - height - 2) + 1;
            Room newRoom = new Room(width, height, x, y);
            Iterable<Point> points = newRoom.roomPoints();
            if (!isColliding(points)) {
                rooms.add(newRoom);
                markUsed(points);
                placeRoom(newRoom);
            }
        }
    }

    private ArrayList<Point> findPath(Point start, Point goal) {
        long currentTime = System.currentTimeMillis();

        // Check cache first
        String cacheKey = start.x + "," + start.y + ":" + goal.x + "," + goal.y;
        if (pathCache.containsKey(cacheKey) && currentTime - lastPathCalculation < PATH_CACHE_DURATION) {
            return new ArrayList<>(pathCache.get(cacheKey)); // Convert List to ArrayList
        }

        // Original path finding logic
        Map<Point, Double> gScore = new HashMap<>();
        PriorityQueue<Point> openSet = new PriorityQueue<>(
                Comparator.comparingDouble(p -> gScore.getOrDefault(p, Double.POSITIVE_INFINITY)));
        Set<Point> closedSet = new HashSet<>();
        Map<Point, Point> comeFrom = new HashMap<>();
        openSet.add(start);
        gScore.put(start, 0.0);

        while (!openSet.isEmpty()) {
            Point current = openSet.poll();
            if (current.equals(goal)) {
                ArrayList<Point> path = constructPath(comeFrom, current);
                // Store in cache
                pathCache.put(cacheKey, new ArrayList<>(path));
                lastPathCalculation = currentTime;
                return path;
            }

            closedSet.add(current);

            for (Point neighbour : getNeighbour(current)) {
                if (closedSet.contains(neighbour)) {
                    continue;
                }

                double tentativeGScore = gScore.get(current) + 1;

                if (!openSet.contains(neighbour)) {
                    openSet.add(neighbour);
                } else if (tentativeGScore >= gScore.get(neighbour)) {
                    continue;
                }

                gScore.put(neighbour, tentativeGScore);
                comeFrom.put(neighbour, current);
            }
        }
        return null;
    }

    private List<Point> getNeighbour(Point p) {
        List<Point> neighbours = new ArrayList<>();
        int[][] directions = { { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 } }; // Up, Right, Down, Left

        for (int[] d : directions) {
            int newX = p.x + d[0];
            int newY = p.y + d[1];
            Point newPoint = new Point(newX, newY);
            // Allow the neighbor if it's either walkable OR it's the avatar's position
            if (isWalkable(newX, newY) ||
                    (newX == avatarX && newY == avatarY)) {
                neighbours.add(newPoint);
            }
        }
        return neighbours;
    }

    private ArrayList<Point> constructPath(Map<Point, Point> comeFrom, Point current) {
        ArrayList<Point> path = new ArrayList<>();
        path.add(current);
        while (comeFrom.containsKey(current)) {
            current = comeFrom.get(current);
            path.add(current);
        }
        // Only remove the starting position (chaser's current position)
        path.removeLast();
        Collections.reverse(path);
        return path;
    }

    private void markUsed(Iterable<Point> points) {
        for (Point p : points) {
            usedSpaces.add(p);
        }
    }

    private boolean isColliding(Iterable<Point> points) {
        for (Point p : points) {
            if (usedSpaces.contains(p)) {
                return true;
            }
        }
        return false;
    }

    private void placeRoom(Room room) {
        int x = room.getPositionX();
        int y = room.getPositionY();
        int width = room.getWidth();
        int height = room.getHeight();
        for (int i = x + 1; i < x + width; i++) {
            for (int j = y + 1; j < y + height; j++) {
                map[i][j] = FLOOR;
            }
        }
        for (int i = x; i <= x + width; i++) {
            map[i][y] = WALL;
            map[i][y + height] = WALL;
        }
        for (int j = y; j <= y + height; j++) {
            map[x][j] = WALL;
            map[x + width][j] = WALL;
        }
    }

    // should check if place hallway is successful, if not. connect in another
    // hallway.
    public void connectRooms(Room room1, Room room2) {
        Hallway hallway;
        if (room1.getPositionY() > room2.getPositionY()) {
            hallway = createHallway(room2, room1);

        } else {
            hallway = createHallway(room1, room2);
        }
        placeHallway(hallway);
        hallways.add(hallway);
    }

    private Hallway createHallway(Room room1, Room room2) {
        int x1 = room1.getPositionX() + room1.getWidth() / 2;
        int y1 = room1.getPositionY() + room1.getHeight() / 2;
        Hallway hallway = new Hallway();
        // vertical straight hallway
        if (x1 >= room2.getPositionX() + 2 && x1 <= room2.getPositionX() + room2.getWidth() - 2) {
            // room2 is above room1
            if (y1 < room2.getPositionY()) {
                hallway = new StraightHallway(x1, y1 + room1.getHeight() / 2, x1, room2.getPositionY());
            } else if (y1 > room2.getPositionY()) {
                hallway = new StraightHallway(x1, y1 - room1.getHeight() / 2, x1,
                        room2.getPositionY() + room2.getHeight());
            }
            // horizontal straight hallway
        } else if (y1 >= room2.getPositionY() + 2 && y1 <= room2.getPositionY() + room2.getHeight() - 2) {
            // room2 is on the left of room1
            if (x1 > room2.getPositionX()) {
                hallway = new StraightHallway(x1 - room1.getWidth() / 2, y1, room2.getPositionX() + room2.getWidth(),
                        y1);
            } else if (x1 < room2.getPositionX()) {
                hallway = new StraightHallway(x1 + room1.getWidth() / 2, y1, room2.getPositionX(), y1);
            }
        }
        // create turn hallway
        else {
            int midX = room2.getPositionX();
            int midY = room1.getPositionY();
            if (room1.getPositionX() <= room2.getPositionX()) {
                // room2 is on the right of room1
                hallway = new TurnHallway(room1.getPositionX() + room1.getWidth() - 1, room1.getPositionY(), midX, midY,
                        room2.getPositionX(), room2.getPositionY());

            } else {
                // room2 is on the left of room1
                hallway = new TurnHallway(room1.getPositionX(), room1.getPositionY(), midX, midY, room2.getPositionX(),
                        room2.getPositionY());
            }
        }
        return hallway;
    }

    private void placeHallway(Hallway hallway) {
        if (hallway instanceof StraightHallway) {
            placeStraightHallway(hallway);
        } else {
            // turn hallway
            TurnHallway turnHallway = (TurnHallway) hallway;
            placeTurnHallway(turnHallway);
        }
    }

    private void placeTurnHallway(TurnHallway hallway) {
        int x1 = hallway.startX;
        int x2 = hallway.getMidX();
        int y1 = hallway.startY;
        int y2 = hallway.endY;
        if (x1 <= x2) {
            // room2 is on the right,above room1
            // floor
            drawLShape(x1, x2 + 1, y1 + 1, y2, FLOOR, false);
            // lower wall
            drawLShape(x1, x2 + 2, y1, y2, WALL, false);
            // upper wall
            drawLShape(x1, x2, y1 + 2, y2, WALL, false);
        } else {
            // room2 is on the left, above room1
            // floor
            drawLShape(x2 + 1, x1, y1 + 1, y2, FLOOR, true);
            // lower wall
            drawLShape(x2, x1, y1, y2, WALL, true);
            // upper wall
            drawLShape(x2 + 2, x1, y1 + 2, y2, WALL, true);
        }

    }

    private void drawLShape(int smallX, int bigX, int smallY, int bigY, TETile tileSet, boolean isBasedOnSmallX) {
        // draw the horizontal first
        int i, j;
        for (i = smallX; i <= bigX; i++) {
            if (tileSet == FLOOR) {
                map[i][smallY] = tileSet;
            } else if (map[i][smallY] != FLOOR) {
                map[i][smallY] = tileSet;
            }
        }
        // then the vertical part
        for (j = smallY; j <= bigY; j++) {
            int x = smallX;
            if (!isBasedOnSmallX) {
                x = bigX;
            }
            if (tileSet == FLOOR) {
                map[x][j] = tileSet;
            } else if (map[x][j] != FLOOR) {
                map[x][j] = tileSet;
            }
        }
    }

    private void placeStraightHallway(Hallway hallway) {
        if (hallway.isVertical()) {
            for (int i = hallway.startX; i <= hallway.startX + 2; i += 2) {
                for (int j = hallway.startY; j <= hallway.endY; j++) {
                    if (map[i][j] != FLOOR) {
                        map[i][j] = WALL;
                    }
                }
            }
            for (int j = hallway.startY; j <= hallway.endY; j++) {
                map[hallway.startX + 1][j] = FLOOR;
            }
        } else {
            int startX = hallway.startX;
            int endX = hallway.endX;
            if (startX > endX) {
                int temp = startX;
                startX = endX;
                endX = temp;
            }
            for (int i = startX; i <= endX; i++) {
                for (int j = hallway.startY; j <= hallway.startY + 2; j += 2) {
                    if (map[i][j] != FLOOR) {
                        map[i][j] = WALL;
                    }
                }
            }
            for (int i = startX; i <= endX; i++) {
                map[i][hallway.startY + 1] = FLOOR;
            }
        }
    }

    private void placeConsumables() {

    }

    public TETile[][] getMap() {
        return map;
    }

    public int getAvatarX() {
        return avatarX;
    }

    public int getAvatarY() {
        return avatarY;
    }

    public long getSeed() {
        return seed;
    }

    public boolean isShowPath() {
        return isShowPath;
    }

    public List<Point> getPathToAvatar() {
        return pathToAvatar;
    }

    public int getChaserX() {
        return chaserX;
    }

    public int getChaserY() {
        return chaserY;
    }

    public int getDoorX() {
        return doorX;
    }

    public int getDoorY() {
        return doorY;
    }

    public Set<Point> getConsumablePositions() {
        return consumablePositions;
    }

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    private void generateWorld(int numConsumables, int numObstacles) {
        // Modify your existing world generation to use these parameters
        // Add more obstacles and consumables based on the parameters
        // This allows for increasing difficulty in higher levels
    }

    private void handleObstacle(ObstacleType obstacle, Point position) {
        switch (obstacle) {
            case SPIKES:
                player.addPoints(obstacle.getPointPenalty());
                AudioManager.getInstance().playSound("damage");
                eventDispatcher.dispatch(new Event(Event.EventType.OBSTACLE_HIT,
                        "Ouch! Lost " + Math.abs(obstacle.getPointPenalty()) + " points!"));
                break;

            case TELEPORTER:
                List<Point> validSpots = getValidTeleportLocations();
                if (!validSpots.isEmpty()) { // Add check for empty list
                    Point newLocation = validSpots.get(random.nextInt(validSpots.size()));
                    setAvatarToNewPosition(newLocation.x, newLocation.y);
                    AudioManager.getInstance().playSound("teleport");
                    eventDispatcher.dispatch(new Event(Event.EventType.OBSTACLE_HIT,
                            "Teleported!"));
                } else {
                    // Fallback if no valid spots found
                    eventDispatcher.dispatch(new Event(Event.EventType.OBSTACLE_HIT,
                            "Teleporter malfunctioned!"));
                }
                break;

            case ICE:
                handleIceSlide(position);
                AudioManager.getInstance().playSound("slide");
                eventDispatcher.dispatch(new Event(Event.EventType.OBSTACLE_HIT,
                        "Sliding on ice!"));
                break;

            case DARK_MODE:
                handleDarkRoom(position);
                break;
        }
    }

    private List<Point> getValidTeleportLocations() {
        List<Point> validSpots = new ArrayList<>();

        // Check all positions in the world
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                Point point = new Point(x, y);

                // Check multiple conditions for a valid teleport location
                if (map[x][y] == FLOOR && // Must be a floor tile
                        !obstacles.containsKey(point) && // No obstacles
                        !consumablePositions.contains(point) && // No consumables
                        (Math.abs(x - chaserX) > 5 || Math.abs(y - chaserY) > 5) && // Not too close to chaser
                        (Math.abs(x - doorX) > 3 || Math.abs(y - doorY) > 3) && // Not too close to door
                        (x != avatarX || y != avatarY)) { // Not current position
                    validSpots.add(point);
                }
            }
        }

        return validSpots;
    }

    private void handleIceSlide(Point position) {
        int dx = 0;
        int dy = 0;

        // Determine the direction based on the last movement
        switch (lastDirection) {
            case 'w' -> dy = 1; // Up
            case 's' -> dy = -1; // Down
            case 'a' -> dx = -1; // Left
            case 'd' -> dx = 1; // Right
            default -> {
                return;
            } // Exit if no valid direction
        }

        int newX = position.x;
        int newY = position.y;

        // Slide until hitting a wall or non-ice tile
        while (true) {
            int nextX = newX + dx;
            int nextY = newY + dy;

            // Check bounds and walls first
            if (nextX < 0 || nextX >= WIDTH || nextY < 0 || nextY >= HEIGHT ||
                    map[nextX][nextY] == WALL) {
                break;
            }

            // Move to next position if it's ice or floor
            newX = nextX;
            newY = nextY;

            // Optional delay for sliding effect
            try {
                Thread.sleep(50);
                setAvatarToNewPosition(newX, newY);
            } catch (InterruptedException e) {
                // Handle interruption
            }
        }

        // Final position update along with checking for obstacles
        setAvatarToNewPosition(newX, newY);
        checkDarkModeProximity(); // <-- New: check proximity after sliding finishes
    }

    public TETile getFloorTile() {
        return FLOOR;
    }

    public TETile getWallTile() {
        return WALL;
    }

    public List<Consumable> getConsumables() {
        return consumables;
    }

    public TETile[][] getVisibleMap() {
        if (!isDarkMode) {
            return map;
        }

        long currentTime = System.currentTimeMillis();

        // Update path flash state
        if (currentTime - lastPathFlashTime >= PATH_FLASH_INTERVAL) {
            lastPathFlashTime = currentTime;
            showPathThisFrame = !showPathThisFrame; // Toggle path visibility
        }

        // Create a new map filled with darkness
        visibleMap = new TETile[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                visibleMap[x][y] = Tileset.NOTHING; // Dark tiles
            }
        }

        // Show tiles within vision radius of avatar
        for (int x = Math.max(0, avatarX - visionRadius); x < Math.min(WIDTH, avatarX + visionRadius + 1); x++) {
            for (int y = Math.max(0, avatarY - visionRadius); y < Math.min(HEIGHT, avatarY + visionRadius + 1); y++) {
                double distance = Math.sqrt(Math.pow(x - avatarX, 2) + Math.pow(y - avatarY, 2));
                if (distance <= visionRadius) {
                    visibleMap[x][y] = map[x][y];
                }
            }
        }

        // During flash, show chaser and door regardless of distance
        // Show chaser
        visibleMap[chaserX][chaserY] = map[chaserX][chaserY];

        // Show door
        visibleMap[doorX][doorY] = map[doorX][doorY];

        // Only show path if player is not invisible and path exists
        if (!player.isInvisible() && pathToAvatar != null && !pathToAvatar.isEmpty() && showPathThisFrame) {
            for (Point p : pathToAvatar) {
                // Only set the path tile if it's not the chaser's position or avatar position
                if (!((p.x == chaserX && p.y == chaserY) || (p.x == avatarX && p.y == avatarY))) {
                    visibleMap[p.x][p.y] = Tileset.PATH;
                }
            }
        }

        return visibleMap;
    }

    private void handleDarkRoom(Point position) {
        isDarkMode = true;
        visionRadius = 3; // Severely reduced vision
        AudioManager.getInstance().playSound("darkness");
        eventDispatcher.dispatch(new Event(Event.EventType.OBSTACLE_HIT,
                "Darkness engulfs you! Find a torch or exit to restore light!"));

        // Place torch near the dark room
        placeTorchNearDarkRoom(position);
    }

    private void placeTorchNearDarkRoom(Point darkRoomPosition) {
        // Define possible adjacent positions (up, down, left, right)
        Point[] adjacentPositions = {
                new Point(darkRoomPosition.x + 2, darkRoomPosition.y), // Right
                new Point(darkRoomPosition.x - 2, darkRoomPosition.y), // Left
                new Point(darkRoomPosition.x, darkRoomPosition.y + 2), // Up
                new Point(darkRoomPosition.x, darkRoomPosition.y - 2) // Down
        };

        // Try to place torch in one of these positions
        for (Point p : adjacentPositions) {
            if (isValidTorchPosition(p)) {
                addTorch(p.x, p.y);
                eventDispatcher.dispatch(new Event(Event.EventType.OBSTACLE_HIT,
                        "A torch glimmers nearby..."));
                return;
            }
        }
    }

    private boolean isValidTorchPosition(Point p) {
        // Check if position is within bounds
        if (p.x < 0 || p.x >= WIDTH || p.y < 0 || p.y >= HEIGHT) {
            return false;
        }

        // Check if position is a floor tile and not occupied by anything else
        return map[p.x][p.y] == FLOOR &&
                !obstacles.containsKey(p) &&
                !consumablePositions.contains(p);
    }

    public void pickupTorch() {
        // Remove the torch from the current position
        Point currentPos = new Point(avatarX, avatarY);
        torchPositions.remove(currentPos);

        visionRadius = 15; // Increased from 7 to 15 for better visibility
        AudioManager.getInstance().playSound("torch");
        eventDispatcher.dispatch(new Event(Event.EventType.ITEM_PICKUP,
                "You found a torch! Your vision greatly improves!"));
    }

    public void exitDarkMode() {
        if (isDarkMode) {
            isDarkMode = false;
            visionRadius = 5; // Reset to default
            eventDispatcher.dispatch(new Event(Event.EventType.OBSTACLE_END,
                    "Light returns to the room!"));
        }
    }

    public void checkDarkModeProximity() {
        // Get the AudioManager instance
        AudioManager audioManager = AudioManager.getInstance();

        // If master volume is 0, don't play any sounds
        if (audioManager.getMasterVolume() <= 0.001f) {
            if (isEerieSoundPlaying) {
                audioManager.stopSound("eerie");
                isEerieSoundPlaying = false;
            }
            return;
        }

        boolean isNear = false;
        Point avatarPos = new Point(avatarX, avatarY);

        for (Map.Entry<Point, ObstacleType> entry : obstacles.entrySet()) {
            if (entry.getValue() == ObstacleType.DARK_MODE) {
                List<Point> path = findPath(avatarPos, entry.getKey());
                // If path exists and is within threshold
                if (path != null && path.size() <= 6) { // Adjusted threshold for path length
                    isNear = true;
                    break;
                }
            }
        }

        if (isNear) {
            if (!isEerieSoundPlaying) {
                audioManager.playSound("eerie");
                isEerieSoundPlaying = true;
            }
        } else {
            if (isEerieSoundPlaying) {
                audioManager.fadeOutSound("eerie", 2000);
                isEerieSoundPlaying = false;
            }
        }
    }

    // Update the checkChaserProximity method to consider invisibility sound effect
    void checkChaserProximity() {
        // Get the AudioManager instance
        AudioManager audioManager = AudioManager.getInstance();

        // If master volume is 0, don't play any sounds
        if (audioManager.getMasterVolume() <= 0.001f) {
            if (isChaserSoundPlaying) {
                audioManager.stopSound("chaser");
                isChaserSoundPlaying = false;
            }
            return;
        }

        // If player is invisible, stop chaser sound regardless of proximity
        if (player.isInvisible()) {
            if (isChaserSoundPlaying) {
                audioManager.fadeOutSound("chaser", 2000);
                isChaserSoundPlaying = false;
            }
            return;
        }

        // Original proximity check logic for visible players
        List<Point> path = findPath(new Point(chaserX, chaserY), new Point(avatarX, avatarY));

        if (path == null || path.isEmpty()) {
            if (isChaserSoundPlaying) {
                audioManager.fadeOutSound("chaser", 2000);
                isChaserSoundPlaying = false;
            }
            return;
        }

        int pathDistance = path.size();

        if (pathDistance <= 8) {
            if (!isChaserSoundPlaying) {
                audioManager.playSound("chaser");
                isChaserSoundPlaying = true;
            }
        } else {
            if (isChaserSoundPlaying) {
                audioManager.fadeOutSound("chaser", 2000);
                isChaserSoundPlaying = false;
            }
        }
    }

    // Helper method to determine if a tile is walkable for the chaser.
    private boolean isWalkable(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            return false;
        }
        if (map[x][y] == WALL) {
            return false;
        }
        return true;
    }

    public void updateAvatarTile() {
        if (player != null) {
            if (player.isInvisible()) {
                map[avatarX][avatarY] = Tileset.INVISIBLE;
            } else {
                TETile[] directionalSet = AvatarTileset.DIRECTIONAL_SETS[player.getAvatarChoice()];

                // Update the avatar tile based on the last direction moved
                switch (Character.toLowerCase(lastDirection)) {
                    case 'w' -> map[avatarX][avatarY] = directionalSet[1]; // Up
                    case 's' -> map[avatarX][avatarY] = directionalSet[0]; // Down
                    case 'a' -> map[avatarX][avatarY] = directionalSet[2]; // Left
                    case 'd' -> map[avatarX][avatarY] = directionalSet[3]; // Right
                    default -> map[avatarX][avatarY] = directionalSet[0]; // Default to front
                }
            }
        }
    }

    public void addConsumable(int x, int y, String type) {
        // Create consumable based on type
        Consumable consumable;
        if (type.equals("Smiley Face")) {
            consumable = new Consumable("Smiley Face", 10, Tileset.SMILEY_FACE_green_body_circle);
        } else {
            consumable = new Consumable("Normal Face", 5, Tileset.SMILEY_FACE_green_body_rhombus);
        }

        // Add to lists and map
        consumables.add(consumable);
        consumablePositions.add(new Point(x, y));
        map[x][y] = consumable.getTile();
    }

    public void addObstacle(int x, int y, ObstacleType type) {
        map[x][y] = type.getTile();
        obstacles.put(new Point(x, y), type);
    }

    public List<Point> getConsumablesList() {
        List<Point> consumableList = new ArrayList<>();
        for (Point p : consumablePositions) {
            consumableList.add(p);
        }
        return consumableList;
    }

    public Map<Point, ObstacleType> getObstacleMap() {
        return obstacles;
    }

    public void setDoorPosition(int x, int y) {
        doorX = x;
        doorY = y;
        // Also update the map tile to show the door
        map[x][y] = Tileset.LOCKED_DOOR;
    }

    public void resetDoorState() {
        if (map[doorX][doorY] == Tileset.UNLOCKED_DOOR) {
            map[doorX][doorY] = Tileset.LOCKED_DOOR;
        }
    }

    // Add this method to handle consumables
    private void handleConsumable(Point pos, TETile tileAtPosition) {
        for (Consumable consumable : consumables) {
            if (tileAtPosition == consumable.getTile()) {
                AudioManager.getInstance().playSound("consume");
                player.addPoints(consumable.getPointValue());
                eventDispatcher.dispatch(new Event(Event.EventType.CONSUMABLE_CONSUMED,
                        "You got " + consumable.getPointValue() + " points!"));
                consumablePositions.remove(pos);
                break;
            }
        }
    }

    public static int getHEIGHT() {
        return HEIGHT;
    }

    public static int getWIDTH() {
        return WIDTH;
    }

    // Add method to handle sound state
    public void stopChaserSound() {
        AudioManager.getInstance().stopSound("chaser");
        isChaserSoundPlaying = false; // Update the state flag
    }

    public boolean isDarkMode() {
        return isDarkMode;
    }

    public void setDarkMode(boolean darkMode, int visionRadius) {
        this.isDarkMode = darkMode;
        this.visionRadius = visionRadius;
    }

    public int getVisionRadius() {
        return visionRadius;
    }

    public void addTorch(int x, int y) {
        torchPositions.add(new Point(x, y));
        map[x][y] = Tileset.TORCH;
    }

    public List<Point> getTorchPositions() {
        return new ArrayList<>(torchPositions);
    }

    public boolean isChaserIsDead() {
        return chaserIsDead;
    }

    public void setChaserIsDead(boolean chaserIsDead) {
        this.chaserIsDead = chaserIsDead;
    }

    // Add a method to reset sound flags
    public void resetSoundFlags() {
        isEerieSoundPlaying = false;
        isChaserSoundPlaying = false;
    }
}
