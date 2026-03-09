package com.example.cardgame;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * JavaFX controller for the Card 24 game screen.
 * <p>
 * Responsibilities:
 * - Randomize and render four cards from a standard deck.
 * - Validate a player expression against the displayed card values.
 * - Evaluate arithmetic expressions with parentheses and +, -, *, / operators.
 * - Optionally find and display one valid solution for the current hand.
 */
public class Card24Controller {
    private static final double TARGET = 24.0;
    private static final double EPSILON = 1.0e-9;
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("0.########");

    @FXML
    private ImageView cardImage1;
    @FXML
    private ImageView cardImage2;
    @FXML
    private ImageView cardImage3;
    @FXML
    private ImageView cardImage4;
    @FXML
    private Label cardValuesLabel;
    @FXML
    private TextField solutionField;
    @FXML
    private TextField expressionField;

    private final Random random = new Random();
    private final List<Card> deck = buildDeck();
    private List<Card> currentHand = List.of();
    private List<ImageView> cardViews;

    /**
     * Wires image views and loads the first random hand when FXML is ready.
     */
    @FXML
    private void initialize() {
        cardViews = List.of(cardImage1, cardImage2, cardImage3, cardImage4);
        refreshCards();
    }

    /**
     * Verifies the player's expression:
     * - all four card values are used exactly once
     * - expression is syntactically valid
     * - final numeric value equals 24
     */
    @FXML
    private void verifyExpression() {
        String expression = expressionField.getText();
        if (expression == null || expression.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Verification Failed", "Enter an expression before verifying.");
            return;
        }

        try {
            List<Token> tokens = tokenize(expression);
            validateCardUsage(tokens);
            double value = evaluateTokens(tokens);

            if (Math.abs(value - TARGET) < EPSILON) {
                showAlert(Alert.AlertType.INFORMATION, "Correct", "Great job. Your expression evaluates to 24.");
            } else {
                showAlert(
                        Alert.AlertType.ERROR,
                        "Not 24",
                        "Your expression evaluates to " + NUMBER_FORMAT.format(value) + ", not 24."
                );
            }
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.ERROR, "Verification Failed", ex.getMessage());
        }
    }

    /**
     * Computes and displays one valid expression that reaches 24 for the current hand.
     */
    @FXML
    private void findSolution() {
        String solution = solve24(currentHand.stream().map(Card::value).toList());
        if (solution == null) {
            solutionField.setText("No solution found");
            return;
        }
        solutionField.setText(solution);
    }

    /**
     * Shuffles the deck, selects four cards, updates the UI, and clears user input fields.
     */
    @FXML
    private void refreshCards() {
        List<Card> shuffled = new ArrayList<>(deck);
        Collections.shuffle(shuffled, random);
        currentHand = List.copyOf(shuffled.subList(0, 4));

        for (int i = 0; i < cardViews.size(); i++) {
            cardViews.get(i).setImage(loadImage(currentHand.get(i).resourcePath()));
        }

        String values = currentHand.stream()
                .map(card -> Integer.toString(card.value()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("");

        cardValuesLabel.setText("Card values: " + values);
        solutionField.clear();
        expressionField.clear();
    }

    /**
     * Loads a card image from the classpath.
     *
     * @param resourcePath classpath location, for example {@code /playing-cards/spades_A.png}
     * @return the loaded JavaFX image
     */
    private Image loadImage(String resourcePath) {
        URL resource = getClass().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Missing card image: " + resourcePath);
        }
        return new Image(resource.toExternalForm());
    }

    /**
     * Ensures the expression uses exactly four numbers and that they match the displayed cards.
     *
     * @param tokens tokenized expression
     */
    private void validateCardUsage(List<Token> tokens) {
        List<Integer> usedValues = tokens.stream()
                .filter(token -> token.type() == TokenType.NUMBER)
                .map(token -> Integer.parseInt(token.text()))
                .sorted(Comparator.naturalOrder())
                .toList();

        List<Integer> expectedValues = currentHand.stream()
                .map(Card::value)
                .sorted(Comparator.naturalOrder())
                .toList();

        if (usedValues.size() != 4) {
            throw new IllegalArgumentException("Use all four card values exactly once.");
        }

        if (!usedValues.equals(expectedValues)) {
            throw new IllegalArgumentException("Expression must use these exact values once each: " + expectedValues);
        }
    }

    /**
     * Evaluates an infix token stream by:
     * 1) converting to postfix (Reverse Polish Notation) with the shunting-yard algorithm
     * 2) evaluating postfix with a numeric stack
     *
     * @param tokens tokenized infix expression
     * @return numeric result of the expression
     */
    private double evaluateTokens(List<Token> tokens) {
        List<Token> output = new ArrayList<>();
        Deque<Token> operators = new ArrayDeque<>();

        // Convert infix tokens to postfix tokens, respecting precedence and parentheses.
        for (Token token : tokens) {
            switch (token.type()) {
                case NUMBER -> output.add(token);
                case OPERATOR -> {
                    while (!operators.isEmpty()
                            && operators.peek().type() == TokenType.OPERATOR
                            && precedence(operators.peek().text()) >= precedence(token.text())) {
                        output.add(operators.pop());
                    }
                    operators.push(token);
                }
                case LPAREN -> operators.push(token);
                case RPAREN -> {
                    boolean foundLParen = false;
                    while (!operators.isEmpty()) {
                        Token top = operators.pop();
                        if (top.type() == TokenType.LPAREN) {
                            foundLParen = true;
                            break;
                        }
                        output.add(top);
                    }
                    if (!foundLParen) {
                        throw new IllegalArgumentException("Mismatched parentheses in expression.");
                    }
                }
            }
        }

        while (!operators.isEmpty()) {
            Token top = operators.pop();
            if (top.type() == TokenType.LPAREN || top.type() == TokenType.RPAREN) {
                throw new IllegalArgumentException("Mismatched parentheses in expression.");
            }
            output.add(top);
        }

        // Evaluate postfix expression using a stack of numeric operands.
        Deque<Double> stack = new ArrayDeque<>();
        for (Token token : output) {
            if (token.type() == TokenType.NUMBER) {
                stack.push(Double.parseDouble(token.text()));
                continue;
            }

            if (stack.size() < 2) {
                throw new IllegalArgumentException("Invalid expression format.");
            }

            double right = stack.pop();
            double left = stack.pop();
            double result;

            result = switch (token.text()) {
                case "+" -> left + right;
                case "-" -> left - right;
                case "*" -> left * right;
                case "/" -> {
                    if (Math.abs(right) < EPSILON) {
                        throw new IllegalArgumentException("Division by zero is not allowed.");
                    }
                    yield left / right;
                }
                default -> throw new IllegalArgumentException("Unsupported operator: " + token.text());
            };

            stack.push(result);
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression format.");
        }

        return stack.pop();
    }

    /**
     * Returns operator precedence used by the parser.
     *
     * @param operator arithmetic operator symbol
     * @return precedence where multiplication/division are higher than addition/subtraction
     */
    private int precedence(String operator) {
        return ("*".equals(operator) || "/".equals(operator)) ? 2 : 1;
    }

    /**
     * Converts an expression string into a flat list of tokens.
     * Supports integers, whitespace, parentheses, and +, -, *, / operators.
     *
     * @param expression user-provided infix expression
     * @return ordered token list
     */
    private List<Token> tokenize(String expression) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;

        while (i < expression.length()) {
            char c = expression.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (Character.isDigit(c)) {
                int start = i;
                while (i < expression.length() && Character.isDigit(expression.charAt(i))) {
                    i++;
                }
                tokens.add(new Token(TokenType.NUMBER, expression.substring(start, i)));
                continue;
            }

            if (c == '+' || c == '-' || c == '*' || c == '/') {
                tokens.add(new Token(TokenType.OPERATOR, Character.toString(c)));
                i++;
                continue;
            }

            if (c == '(') {
                tokens.add(new Token(TokenType.LPAREN, "("));
                i++;
                continue;
            }

            if (c == ')') {
                tokens.add(new Token(TokenType.RPAREN, ")"));
                i++;
                continue;
            }

            throw new IllegalArgumentException("Invalid character in expression: '" + c + "'.");
        }

        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Expression is empty.");
        }

        return tokens;
    }

    /**
     * Builds all 52 cards with image path and game value mapping:
     * A=1, 2-10 face value, J=11, Q=12, K=13.
     *
     * @return full deck list
     */
    private List<Card> buildDeck() {
        List<Card> cards = new ArrayList<>(52);
        String[] suits = {"clubs", "diamonds", "hearts", "spades"};
        String[] rankKeys = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        int[] values = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};

        for (String suit : suits) {
            for (int i = 0; i < rankKeys.length; i++) {
                String resourcePath = "/playing-cards/" + suit + "_" + rankKeys[i] + ".png";
                cards.add(new Card(resourcePath, values[i]));
            }
        }

        return cards;
    }

    /**
     * Entrypoint to search for any expression that evaluates to 24.
     *
     * @param values current hand values
     * @return expression string or {@code null} when no solution exists
     */
    private String solve24(List<Integer> values) {
        List<ExprValue> start = values.stream()
                .map(value -> new ExprValue(value, Integer.toString(value)))
                .toList();
        return solveRecursive(start);
    }

    /**
     * Backtracking solver that combines two values at a time using all operations.
     *
     * @param values current reduced expression-value set
     * @return first expression that evaluates to 24, or {@code null}
     */
    private String solveRecursive(List<ExprValue> values) {
        if (values.size() == 1) {
            return Math.abs(values.getFirst().value() - TARGET) < EPSILON ? values.getFirst().expr() : null;
        }

        for (int i = 0; i < values.size(); i++) {
            for (int j = i + 1; j < values.size(); j++) {
                List<ExprValue> remaining = new ArrayList<>();
                for (int k = 0; k < values.size(); k++) {
                    if (k != i && k != j) {
                        remaining.add(values.get(k));
                    }
                }

                ExprValue a = values.get(i);
                ExprValue b = values.get(j);
                List<ExprValue> candidates = new ArrayList<>();
                // Order is arbitrary; we return the first valid expression found.
                candidates.add(new ExprValue(a.value() + b.value(), "(" + a.expr() + " + " + b.expr() + ")"));
                candidates.add(new ExprValue(a.value() * b.value(), "(" + a.expr() + " * " + b.expr() + ")"));
                candidates.add(new ExprValue(a.value() - b.value(), "(" + a.expr() + " - " + b.expr() + ")"));
                candidates.add(new ExprValue(b.value() - a.value(), "(" + b.expr() + " - " + a.expr() + ")"));
                if (Math.abs(b.value()) > EPSILON) {
                    candidates.add(new ExprValue(a.value() / b.value(), "(" + a.expr() + " / " + b.expr() + ")"));
                }
                if (Math.abs(a.value()) > EPSILON) {
                    candidates.add(new ExprValue(b.value() / a.value(), "(" + b.expr() + " / " + a.expr() + ")"));
                }

                for (ExprValue candidate : candidates) {
                    List<ExprValue> next = new ArrayList<>(remaining);
                    next.add(candidate);
                    String result = solveRecursive(next);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Shows a modal feedback dialog for validation and game results.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Immutable card model containing image resource path and numeric game value.
     *
     * @param resourcePath classpath location to the card image
     * @param value card value for arithmetic
     */
    private record Card(String resourcePath, int value) {
        private Card {
            Objects.requireNonNull(resourcePath, "resourcePath");
        }
    }

    /**
     * Basic expression token used by parser and evaluator.
     *
     * @param type token category
     * @param text original token text
     */
    private record Token(TokenType type, String text) {
    }

    /**
     * Intermediate value used by the solver with its generated expression.
     *
     * @param value computed numeric value
     * @param expr expression text that produced {@code value}
     */
    private record ExprValue(double value, String expr) {
    }

    /**
     * Token categories supported by the game expression grammar.
     */
    private enum TokenType {
        NUMBER,
        OPERATOR,
        LPAREN,
        RPAREN
    }
}
