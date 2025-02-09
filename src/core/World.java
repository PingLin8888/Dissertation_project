package core;

import tileengine.TETile;
import tileengine.Tileset;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

public class World {

    // build your own world!
    final private static int WIDTH = 80;
    final private static int HEIGHT = 45;
    final private static TETile UNUSED = Tileset.NOTHING;
    final static TETile FLOOR = Tileset.GRASS;
    final static TETile WALL = Tileset.WALL;
    final private static long SEEDDefault = 87654L;
    final private static TETile AVATAR = Tileset.AVATAR_FRONT;
    final private static TETile CHASER = Tileset.CHASER;
    private int NUMBER_OF_CONSUMABLES = 10;

    private int avatarX, avatarY;
    private int chaserX, chaserY;
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

    private long lastFlashTime = 0;
    private static final long FLASH_INTERVAL = 1000; // 4 seconds in milliseconds
    private boolean isFlashing = false;

    // Field to track whether the eerie sound is currently playing
    private boolean isEerieSoundPlaying = false;
    // Field to track if the chaser sound is currently playing
    private boolean isChaserSoundPlaying = false;

    public World() {
        this(null, SEEDDefault);
    }

    public World(Player player, Long seed) {
        this(player, seed, 10, 10);
    }

    public World(Player player, long seed, int numConsumables, int numObstacles) {
        this.player = player;
        this.seed = seed;
        rooms = new ArrayList<>();
        hallways = new ArrayList<>();
        random = new Random(seed);
        usedSpaces = new HashSet<>();
        this.consumables = new ArrayList<>(); // Initialize the list
        this.consumablePositions = new HashSet<>(); // Initialize the set
        this.eventDispatcher = new EventDispatcher(); // Initialize the event dispatcher
        map = new TETile[WIDTH][HEIGHT];
        initializeWorldWithTiles();
        placeAvatar();
        placeChaser();
        placeDoor();
        populateConsumables(numConsumables); // Call a method to populate the list
        populateObstacles(numObstacles);

    }

    private void populateConsumables(int numConsumables) {
        NUMBER_OF_CONSUMABLES = numConsumables;
        consumables.add(new Consumable("Smiley Face", 10, Tileset.SMILEY_FACE_green_body_circle));
        consumables.add(new Consumable("Normal Face", 5, Tileset.SMILEY_FACE_green_body_rhombus));
        // consumables.add(new Consumable("Angry Face", -5, Tileset.ANGRY_FACE));

        Random rand = new Random();

        // Create a list of available positions from usedSpaces
        List<Point> availablePositions = new ArrayList<>(usedSpaces);

        // Filter available positions to only include floor tiles and exclude the
        // avatar's and chaser's positions
        availablePositions.removeIf(point -> map[point.x][point.y] != FLOOR ||
                (point.x == avatarX && point.y == avatarY) ||
                (point.x == chaserX && point.y == chaserY));

        for (int i = 0; i < NUMBER_OF_CONSUMABLES; i++) {
            if (availablePositions.isEmpty()) {
                break; // Exit if there are no available positions
            }
            Point position = availablePositions.get(rand.nextInt(availablePositions.size()));
            Consumable consumable = consumables.get(rand.nextInt(consumables.size()));
            map[position.x][position.y] = consumable.getTile();
            consumablePositions.add(position); // Store the position of the consumable
            availablePositions.remove(position);
        }
    }

    private void placeAvatar() {
        List<Point> availablePositions = new ArrayList<>(usedSpaces); // Create a copy

        // Filter available positions to only include floor tiles
        availablePositions.removeIf(point -> map[point.x][point.y] != FLOOR);

        // Randomly select one of the available positions for the avatar
        if (!availablePositions.isEmpty()) {
            Random rand = new Random();
            Point randomPosition = availablePositions.get(rand.nextInt(availablePositions.size()));
            avatarX = randomPosition.x;
            avatarY = randomPosition.y;
            map[avatarX][avatarY] = AVATAR;
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
                if (map[neighbor.x][neighbor.y] == FLOOR || map[neighbor.x][neighbor.y] == AVATAR) {
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
            // Check for collision after random movement.
            if (chaserX == avatarX && chaserY == avatarY) {
                if (!player.isInvisible()) {
                    AudioManager.getInstance().stopSound("chaser");
                    eventDispatcher.dispatch(new Event(Event.EventType.GAME_OVER, "The chaser caught you!"));
                }
            }
        } else {
            // Normal chasing mode.
            pathToAvatar = findPath(new Point(chaserX, chaserY), new Point(avatarX, avatarY));
            if (pathToAvatar != null && !pathToAvatar.isEmpty()) {
                Point next = pathToAvatar.getFirst();
                setChaserToNewPosition(next.x, next.y);
                checkChaserProximity();
                if (chaserX == avatarX && chaserY == avatarY) {
                    if (!player.isInvisible()) {
                        AudioManager.getInstance().stopSound("chaser");
                        eventDispatcher.dispatch(new Event(Event.EventType.GAME_OVER, "The chaser caught you!"));
                    }
                }
            }
        }
    }

    public void setChaserToNewPosition(int x, int y) {
        map[chaserX][chaserY] = FLOOR;
        chaserX = x;
        chaserY = y;
        map[chaserX][chaserY] = CHASER;
    }

    public void togglePathDisplay() {
        isShowPath = !isShowPath;
    }

    public boolean moveAvatar(char direction) {
        // Set the last direction before processing the move
        lastDirection = direction;

        int newX = avatarX;
        int newY = avatarY;
        switch (Character.toLowerCase(direction)) {
            case 'w' -> newY += 1;
            case 'a' -> newX -= 1;
            case 's' -> newY -= 1;
            case 'd' -> newX += 1;
        }

        if (newX >= 0 && newX < WIDTH && newY >= 0 && newY < HEIGHT) {
            TETile tileAtNewPosition = map[newX][newY];

            // Handle collision with chaser:
            if (tileAtNewPosition == CHASER) {
                // Check if player has purchased and activated invisibility cure.
                if (!player.isInvisible()) {
                    // Stop chaser sound immediately when caught.
                    AudioManager.getInstance().stopSound("chaser");
                    eventDispatcher.dispatch(new Event(Event.EventType.GAME_OVER, "The chaser caught you!"));
                    return false;
                }
                // else allow safe passage if invisible
            }

            // Check for torch pickup
            if (tileAtNewPosition == Tileset.TORCH) {
                pickupTorch();
                map[newX][newY] = FLOOR;
                setAvatarToNewPosition(newX, newY);
                return true;
            }

            Point newPos = new Point(newX, newY);
            if (obstacles.containsKey(newPos)) {
                handleObstacle(obstacles.get(newPos), newPos);
                obstacles.remove(newPos);
                map[newX][newY] = FLOOR;
                return true;
            }

            // Check for consumables
            for (Consumable consumable : consumables) {
                if (tileAtNewPosition == consumable.getTile()) {
                    AudioManager.getInstance().playSound("consume");
                    player.addPoints(consumable.getPointValue());
                    map[newX][newY] = FLOOR;
                    eventDispatcher.dispatch(new Event(Event.EventType.CONSUMABLE_CONSUMED,
                            "You got " + consumable.getPointValue() + " points!"));
                    break;
                }
            }

            // If the new position is not a wall, move is successful
            if (tileAtNewPosition != WALL) {
                if (tileAtNewPosition == Tileset.LOCKED_DOOR) {
                    map[newX][newY] = Tileset.UNLOCKED_DOOR; // Change to unlocked door
                }
                setAvatarToNewPosition(newX, newY);
                checkDarkModeProximity();
                checkChaserProximity();
                return true; // Move was successful
            }
        }
        return false; // Move was blocked
    }

    public void setAvatarToNewPosition(int x, int y) {
        map[avatarX][avatarY] = FLOOR;
        avatarX = x;
        avatarY = y;
        if (player != null && player.isInvisible()) {
            // Optionally, you could also have directional invisible avatars here.
            map[avatarX][avatarY] = Tileset.AVATAR_INVISIBLE;
        } else {
            // Update the avatar tile based on the last direction moved.
            switch (Character.toLowerCase(lastDirection)) {
                case 'w':
                    map[avatarX][avatarY] = Tileset.AVATAR_BACK; // Avatar facing upward
                    break;
                case 's':
                    map[avatarX][avatarY] = Tileset.AVATAR_FRONT; // Avatar facing downward
                    break;
                case 'a':
                    map[avatarX][avatarY] = Tileset.AVATAR_LEFT; // Avatar facing left (side view)
                    break;
                case 'd':
                    map[avatarX][avatarY] = Tileset.AVATAR_Right; // Avatar facing right (side view) using updated
                                                                  // constant
                    break;
                default:
                    map[avatarX][avatarY] = Tileset.AVATAR_FRONT;
                    break;
            }
        }
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
                return constructPath(comeFrom, current);
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

    private void populateObstacles(int numObstacles) {
        Random rand = new Random(seed);
        List<Point> availablePositions = new ArrayList<>(usedSpaces);

        // Filter available positions to only include floor tiles
        availablePositions.removeIf(point -> map[point.x][point.y] != FLOOR ||
                (point.x == avatarX && point.y == avatarY) ||
                (point.x == chaserX && point.y == chaserY));

        // Ensure at least some dark rooms (20% of obstacles will be dark rooms)
        int numDarkRooms = Math.max(1, numObstacles / 5);

        // Place dark rooms first
        for (int i = 0; i < numDarkRooms && !availablePositions.isEmpty(); i++) {
            Point position = availablePositions.get(rand.nextInt(availablePositions.size()));
            obstacles.put(position, ObstacleType.DARK_MODE);
            map[position.x][position.y] = ObstacleType.DARK_MODE.getTile();

            availablePositions.remove(position);
        }

        // Place other obstacles
        for (int i = numDarkRooms; i < numObstacles && !availablePositions.isEmpty(); i++) {
            Point position = availablePositions.get(rand.nextInt(availablePositions.size()));
            ObstacleType obstacle = ObstacleType.values()[rand.nextInt(ObstacleType.values().length - 1)]; // -1 to
                                                                                                           // exclude
                                                                                                           // DARK_ROOM

            obstacles.put(position, obstacle);
            map[position.x][position.y] = obstacle.getTile();
            availablePositions.remove(position);
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
        isFlashing = (currentTime - lastFlashTime) < 500; // Flash lasts 0.5 seconds

        if (currentTime - lastFlashTime >= FLASH_INTERVAL) {
            lastFlashTime = currentTime;
            AudioManager.getInstance().playSound("flash");
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
        if (isFlashing) {
            // Show chaser
            visibleMap[chaserX][chaserY] = map[chaserX][chaserY];

            // Show door
            visibleMap[doorX][doorY] = map[doorX][doorY];

            // Only show path if it exists
            if (pathToAvatar != null && !pathToAvatar.isEmpty()) {
                for (Point p : pathToAvatar) {
                    // Only set the path tile if it's not the chaser's position
                    if (!((p.x == chaserX && p.y == chaserY)||(p.x == avatarX && p.y == avatarY))) {
                        visibleMap[p.x][p.y] = Tileset.PATH;
                    }
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
                map[p.x][p.y] = Tileset.TORCH;
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

    private void checkDarkModeProximity() {
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
                AudioManager.getInstance().playSound("eerie");
                isEerieSoundPlaying = true;
            }
        } else {
            if (isEerieSoundPlaying) {
                AudioManager.getInstance().fadeOutSound("eerie", 2000);
                isEerieSoundPlaying = false;
            }
        }
    }

    // New method to check if the chaser is within 5 tiles of the avatar
    private void checkChaserProximity() {
        // Use pathfinding to get actual distance
        List<Point> path = findPath(new Point(chaserX, chaserY), new Point(avatarX, avatarY));

        // If no path exists or path is null, chaser can't reach avatar
        if (path == null || path.isEmpty()) {
            if (isChaserSoundPlaying) {
                AudioManager.getInstance().fadeOutSound("chaser", 2000);
                isChaserSoundPlaying = false;
            }
            return;
        }

        // Use path length as actual distance
        int pathDistance = path.size();

        // Adjust threshold based on actual path length
        if (pathDistance <= 8) { // Reduced from 15 to 8 since path length is typically longer than direct
                                 // distance
            if (!isChaserSoundPlaying) {
                AudioManager.getInstance().playSound("chaser");
                isChaserSoundPlaying = true;
            }
        } else {
            if (isChaserSoundPlaying) {
                AudioManager.getInstance().fadeOutSound("chaser", 2000);
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
                map[avatarX][avatarY] = Tileset.AVATAR_INVISIBLE;
            } else {
                // Update the avatar tile based on the last direction moved.
                switch (Character.toLowerCase(lastDirection)) {
                    case 'w':
                        map[avatarX][avatarY] = Tileset.AVATAR_BACK; // Avatar facing upward
                        break;
                    case 's':
                        map[avatarX][avatarY] = Tileset.AVATAR_FRONT; // Avatar facing downward
                        break;
                    case 'a':
                        map[avatarX][avatarY] = Tileset.AVATAR_LEFT; // Avatar facing left (side view)
                        break;
                    case 'd':
                        map[avatarX][avatarY] = Tileset.AVATAR_Right; // Avatar facing right (side view)
                        break;
                    default:
                        map[avatarX][avatarY] = Tileset.AVATAR_FRONT; // Default to front-facing
                        break;
                }
            }
        }
    }
}
