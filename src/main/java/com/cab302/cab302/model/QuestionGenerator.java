package com.cab302.cab302.model;

=======
public class QuestionGenerator {
    
    public static String generateBasicAlgebraProblem(int a, int b) {
        String problem = a + " + " + b + " = ?";
        String solution = Integer.toString(a + b);
        return "Problem: " + problem + "\nAnswer: " + solution;
    }

    public static String generateQuadraticEquationProblem(int a, int b, int c) {
        String problem = a + "x^2 + " + b + "x + " + c + " = 0";
        double discriminant = b * b - 4 * a * c;
        String solution;
        if (discriminant > 0) {
            double root1 = (-b + Math.sqrt(discriminant)) / (2 * a);
            double root2 = (-b - Math.sqrt(discriminant)) / (2 * a);
            solution = "Roots: x1 = " + root1 + ", x2 = " + root2;
        } else if (discriminant == 0) {
            double root = -b / (2 * a);
            solution = "One root: x = " + root;
        } else {
            solution = "No real roots";
        }
        return "Problem: " + problem + "\n" + solution;
    }
    public static String generateCalculusProblem(String function, String variable) {
        String problem = "Differentiate the function: " + function + " with respect to " + variable;
        // Placeholder for actual differentiation logic
        String solution = "Solution: (d/d" + variable + ") " + function + " = [differentiated function]";
        return problem + "\n" + solution;
    }
}
 main
