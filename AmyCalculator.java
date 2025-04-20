import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Scientific Calculator Application
 * 
 * @author Mohammed Aamena Mohammed Abdulkarem
 * @version 1.0
 * @since 2024
 */
public class AmyCalculator {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Error setting system look and feel");
            }
            
            CalculatorModel model = new CalculatorModel();
            CalculatorView view = new CalculatorView();
            new CalculatorController(model, view);
            
            model.addObserver(view);
        });
    }
}

// ==================== MODEL ====================

/**
 * Interface for Observer pattern
 */
interface CalculationObserver {
    void updateCalculationResult(String result);
}

/**
 * Interface for Subject in Observer pattern
 */
interface CalculationSubject {
    void addObserver(CalculationObserver observer);
    void removeObserver(CalculationObserver observer);
    void notifyObservers();
}

/**
 * Handles the calculator's business logic and state management
 */
class CalculatorModel implements CalculationSubject {
    private final List<CalculationObserver> observers = new ArrayList<>();
    private String currentResult = "";
    private String errorMessage = null;
    
    @Override
    public void addObserver(CalculationObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(CalculationObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        observers.forEach(observer -> observer.updateCalculationResult(getDisplayValue()));
    }

    /**
     * Evaluates a mathematical expression
     * @param expression The expression to evaluate
     */
    public void evaluateExpression(String expression) {
        try {
            if (expression == null || expression.trim().isEmpty()) {
                currentResult = "";
                errorMessage = null;
                notifyObservers();
                return;
            }
            
            double result = new ExpressionEvaluator().evaluate(expression);
            currentResult = String.format("%.6f", result).replaceAll("\\.?0*$", "");
            errorMessage = null;
        } catch (ArithmeticException e) {
            errorMessage = "Math Error: " + e.getMessage();
        } catch (Exception e) {
            errorMessage = "Syntax Error";
        }
        notifyObservers();
    }
    
    public String getDisplayValue() {
        return errorMessage != null ? errorMessage : currentResult;
    }
    
    public boolean hasError() {
        return errorMessage != null;
    }
    
    public void clear() {
        currentResult = "";
        errorMessage = null;
        notifyObservers();
    }
}

/**
 * Handles expression parsing and evaluation
 */
class ExpressionEvaluator {
    public double evaluate(String expression) {
        List<String> tokens = tokenizeExpression(expression);
        List<String> postfix = convertToPostfix(tokens);
        return evaluatePostfix(postfix);
    }
    
    private List<String> tokenizeExpression(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentNumber = new StringBuilder();
        
        for (char c : expression.toCharArray()) {
            if (Character.isDigit(c) || c == '.') {
                currentNumber.append(c);
            } else if ("+-×÷()".indexOf(c) != -1) {
                if (currentNumber.length() > 0) {
                    tokens.add(currentNumber.toString());
                    currentNumber.setLength(0);
                }
                tokens.add(String.valueOf(c));
            }
        }
        
        if (currentNumber.length() > 0) {
            tokens.add(currentNumber.toString());
        }
        
        return tokens;
    }
    
    private List<String> convertToPostfix(List<String> infixTokens) {
        List<String> postfix = new ArrayList<>();
        Stack<String> operatorStack = new Stack<>();
        
        for (String token : infixTokens) {
            if (isNumeric(token)) {
                postfix.add(token);
            } else if ("(".equals(token)) {
                operatorStack.push(token);
            } else if (")".equals(token)) {
                while (!operatorStack.isEmpty() && !"(".equals(operatorStack.peek())) {
                    postfix.add(operatorStack.pop());
                }
                operatorStack.pop(); // Remove the '('
            } else { // Operator
                while (!operatorStack.isEmpty() && 
                       getPrecedence(operatorStack.peek()) >= getPrecedence(token)) {
                    postfix.add(operatorStack.pop());
                }
                operatorStack.push(token);
            }
        }
        
        while (!operatorStack.isEmpty()) {
            postfix.add(operatorStack.pop());
        }
        
        return postfix;
    }
    
    private double evaluatePostfix(List<String> postfix) {
        Stack<Double> operandStack = new Stack<>();
        
        for (String token : postfix) {
            if (isNumeric(token)) {
                operandStack.push(Double.parseDouble(token));
            } else {
                double b = operandStack.pop();
                double a = operandStack.pop();
                switch (token) {
                    case "+":
                        operandStack.push(a + b);
                        break;
                    case "-":
                        operandStack.push(a - b);
                        break;
                    case "×":
                        operandStack.push(a * b);
                        break;
                    case "÷":
                        if (b == 0) throw new ArithmeticException("Division by zero");
                        operandStack.push(a / b);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown operator: " + token);
                }
            }
        }
        
        if (operandStack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression");
        }
        
        return operandStack.pop();
    }
    
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private int getPrecedence(String operator) {
        switch (operator) {
            case "+":
            case "-":
                return 1;
            case "×":
            case "÷":
                return 2;
            default:
                return 0;
        }
    }
}

// ==================== VIEW ====================

/**
 * Main calculator GUI
 */
class CalculatorView extends JFrame implements CalculationObserver {
    private static final Color DISPLAY_BG = new Color(240, 240, 240);
    private static final Color DISPLAY_FG = Color.BLACK;
    private static final Font DISPLAY_FONT = new Font("Segoe UI", Font.PLAIN, 32);
    
    private final JTextField displayField;
    private final JPanel buttonPanel;
    
    public CalculatorView() {
        configureWindow();
        displayField = createDisplayField();
        buttonPanel = createButtonPanel();
        
        add(displayField, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        
        setupKeyboardSupport();
        setVisible(true);
    }
    
    private void configureWindow() {
    setTitle("⚡ Amy: Quick Calc");
    setSize(400, 600);
    setMinimumSize(new Dimension(350, 500));
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout(8, 8)); // Extra padding
    getContentPane().setBackground(new Color(250, 250, 250)); // Smooth light gray
    setLocationRelativeTo(null);
}

    
private JTextField createDisplayField() {
    JTextField field = new JTextField();
    field.setFont(new Font("Consolas", Font.BOLD, 36));
    field.setHorizontalAlignment(JTextField.RIGHT);
    field.setEditable(false);

    field.setBackground(new Color(245, 245, 255)); // Light lavender background
    field.setForeground(new Color(80, 80, 80));    // Soft dark gray text
    field.setCaretColor(new Color(180, 180, 255)); // Soft blue caret (optional)

    field.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(220, 220, 250), 2), // Light border
        BorderFactory.createEmptyBorder(20, 15, 20, 15)
    ));

    return field;
}


    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 4, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(Color.LIGHT_GRAY);
        
        String[] buttonLabels = {
            "7", "8", "9", "÷",
            "4", "5", "6", "×",
            "1", "2", "3", "-",
            "0", ".", "C", "+",
            "(", ")", "=", "⌫"
        };
        
        for (String label : buttonLabels) {
            JButton button = createCalculatorButton(label);
            panel.add(button);
        }
        
        return panel;
    }
    
  private Color getPrettyColor(String label) {
    if (label.matches("[0-9.]")) {
        return Color.decode("#FFF5E1"); // Soft Peach
    } else if (label.equals("=")) {
        return Color.decode("#D1F2EB"); // Mint Green
    } else if (label.equals("C") || label.equals("⌫")) {
        return Color.decode("#FADBD8"); // Soft Rose
    } else {
        return Color.decode("#E8F0FE"); // Pastel Blue
    }
}

private Color getPrettyHoverColor(String label) {
    if (label.matches("[0-9.]")) {
        return Color.decode("#FFE8C1"); // Brighter Peach
    } else if (label.equals("=")) {
        return Color.decode("#A3E4D7"); // Brighter Mint
    } else if (label.equals("C") || label.equals("⌫")) {
        return Color.decode("#F5B7B1"); // Brighter Rose
    } else {
        return Color.decode("#D2E3FC"); // Brighter Blue
    }
}


private JButton createCalculatorButton(String label) {
    JButton button = new JButton(label);
    button.setFont(new Font("Segoe UI", Font.BOLD, 18));
    button.setFocusPainted(false);
    button.setForeground(Color.DARK_GRAY);
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    button.setOpaque(true);
    button.setBackground(getPrettyColor(label));
    button.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

    // Rounded corners UI
    button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(button.getBackground());
            g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 20, 20);
            super.paint(g2, c);
        }
    });

    // Hover effect
    button.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            button.setBackground(getPrettyHoverColor(label));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            button.setBackground(getPrettyColor(label));
        }
    });

    return button;
}


    
    private void setupKeyboardSupport() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });
        setFocusable(true);
    }
    
    private void handleKeyPress(KeyEvent e) {
        String keyCommand = mapKeyToCommand(e);
        if (keyCommand != null) {
            triggerButtonAction(keyCommand);
        }
    }
    
    private String mapKeyToCommand(KeyEvent e) {
        char keyChar = e.getKeyChar();
        int keyCode = e.getKeyCode();
        
        if (Character.isDigit(keyChar) || keyChar == '.') {
            return String.valueOf(keyChar);
        } else if (keyChar == '+' || keyChar == '-' || keyChar == '*' || keyChar == '/') {
            return String.valueOf(keyChar).replace('*', '×').replace('/', '÷');
        } else if (keyChar == '(' || keyChar == ')') {
            return String.valueOf(keyChar);
        } else if (keyCode == KeyEvent.VK_ENTER) {
            return "=";
        } else if (keyCode == KeyEvent.VK_BACK_SPACE) {
            return "⌫";
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            return "C";
        }
        return null;
    }
    
    public void addButtonActionListener(ActionListener listener) {
        for (Component comp : buttonPanel.getComponents()) {
            if (comp instanceof JButton) {
                ((JButton) comp).addActionListener(listener);
            }
        }
    }
    
    @Override
    public void updateCalculationResult(String result) {
        displayField.setText(result);
    }
    
    public String getDisplayText() {
        return displayField.getText();
    }
    
    private void triggerButtonAction(String command) {
        for (Component comp : buttonPanel.getComponents()) {
            if (comp instanceof JButton && ((JButton) comp).getText().equals(command)) {
                ((JButton) comp).doClick();
                return;
            }
        }
    }
}

// ==================== CONTROLLER ====================

/**
 * Handles user input and coordinates between Model and View
 */
class CalculatorController implements ActionListener {
    private final CalculatorModel model;
    private final CalculatorView view;
    private StringBuilder currentExpression = new StringBuilder();
    
    public CalculatorController(CalculatorModel model, CalculatorView view) {
        this.model = model;
        this.view = view;
        view.addButtonActionListener(this);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        
        switch (command) {
            case "C":
                handleClear();
                break;
            case "=":
                handleEquals();
                break;
            case "⌫":
                handleBackspace();
                break;
            default:
                handleInput(command);
                break;
        }
        
        view.requestFocusInWindow();
    }
    
    private void handleClear() {
        currentExpression.setLength(0);
        model.clear();
    }
    
    private void handleEquals() {
        if (currentExpression.length() > 0) {
            model.evaluateExpression(currentExpression.toString());
            currentExpression.setLength(0);
        }
    }
    
    private void handleBackspace() {
        if (currentExpression.length() > 0) {
            currentExpression.deleteCharAt(currentExpression.length() - 1);
            view.updateCalculationResult(currentExpression.toString());
        }
    }
    
    private void handleInput(String input) {
        currentExpression.append(input);
        view.updateCalculationResult(currentExpression.toString());
    }
}
