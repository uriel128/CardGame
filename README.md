# Card 24 Game (JavaFX)

A JavaFX implementation of the Card 24 game.

## Overview
The app displays four random playing cards. The player must enter an arithmetic expression that:
- Uses all four card values exactly once
- Uses `+`, `-`, `*`, `/`, and optional parentheses
- Evaluates to `24`

## Card Values
- Ace = `1`
- 2 through 10 = face value
- Jack = `11`
- Queen = `12`
- King = `13`

## Features
- Randomly generates 4 cards from a standard 52-card deck
- `Verify` button to validate:
  - Correct card usage (exact four values once each)
  - Valid expression syntax
  - Final result equals 24
- `Refresh` button to draw a new set of cards
- `Find a Solution` button to show one valid solution when available
- CSS styling for a clean UI

## Project Structure
- `src/main/java/com/example/cardgame/Card24Application.java` - JavaFX application entry
- `src/main/java/com/example/cardgame/Card24Controller.java` - game logic and controller
- `src/main/resources/com/example/cardgame/card24-view.fxml` - UI layout
- `src/main/resources/com/example/cardgame/card24.css` - stylesheet
- `src/main/resources/playing-cards/` - card image assets

## Requirements
- Java 25
- Maven 3.8+

## Run
From the project root:

```bash
./mvnw javafx:run
```

## Build
```bash
./mvnw -DskipTests compile
```

