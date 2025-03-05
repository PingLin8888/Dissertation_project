# Maze Explorer: A 2D Tile-Based Adventure Game

## Project Overview

Maze Explorer is an interactive 2D tile-based adventure game built in Java. The game features procedurally generated mazes where players navigate through obstacles, collect items, and avoid a pursuing enemy (chaser) while trying to reach the exit door to advance to the next level.

## Key Features

### Core Gameplay

- **Procedurally Generated Worlds**: Each level features a unique maze layout generated from a seed
- **Progressive Difficulty**: As players advance through levels, the game becomes more challenging with faster enemies and more obstacles
- **Scoring System**: Players earn points by collecting items and completing levels
- **Level Progression**: Successfully complete each level to advance to more challenging ones

### Game Mechanics

- **Player Movement**: Navigate through the maze using WASD controls
- **Enemy AI**: A "chaser" enemy pursues the player through the maze
- **Special Abilities**: Players can use invisibility to temporarily avoid the chaser
- **Consumables**: Collect various items throughout the maze to earn points
- **Obstacles**: Navigate around spikes, ice, and teleporters that affect gameplay
- **Dynamic Lighting**: Some levels feature dark mode with limited visibility and torches

### Technical Features

- **Multilingual Support**: Play the game in English or Chinese
- **Save/Load System**: Save your progress and continue later, with auto-save functionality
- **Customizable Avatar**: Choose from different character appearances
- **Audio System**: Dynamic sound effects based on game events and player actions
- **Pause Functionality**: Pause the game at any time to take a break

### User Interface

- **Animated Menus**: Smooth transitions between different game states
- **Heads-Up Display (HUD)**: Real-time information about player status, points, and environment
- **Notifications**: In-game notifications for important events
- **Tutorial System**: Comprehensive in-game instructions for new players
- **Settings Menu**: Customize game settings to your preference

## Technical Implementation

- Built using Java with the Princeton Standard Draw library for rendering
- Object-oriented design with clear separation of concerns
- Event-driven architecture for game events and notifications
- Efficient data structures for world representation and pathfinding
- File I/O for save/load functionality

## How to Play

Navigate through the maze using WASD keys, collect items to earn points, and find the exit door to advance to the next level. Avoid the chaser enemy or use your invisibility ability to escape. Complete all levels to win the game!
