package com.frauddetect;

import com.frauddetect.model.Admin;
import com.frauddetect.model.Customer;
import com.frauddetect.model.DomesticTransaction;
import com.frauddetect.model.InternationalTransaction;
import com.frauddetect.model.RiskLevel;
import com.frauddetect.model.Transaction;
import com.frauddetect.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Main application entry point for AI Fraud Detection & Transaction Monitoring System.
 * Phase 2 Test: Demonstrates OOP Polymorphism, Inheritance, and Encapsulation across Models.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println(" AI-Powered Financial Fraud Detection System - Phase 2 Verification");
        System.out.println("=========================================================================\n");

        // 1. RUBRIC 1: Polymorphism & Inheritance with User hierarchy
        System.out.println("--- 1. Polymorphic User Demonstration ---");
        List<User> users = new ArrayList<>();
        users.add(new Customer(1, "john_doe", "John Doe", "john@example.com", "India"));
        users.add(new Admin(2, "admin_mahi", "Mahi Dhauni", "admin@frauddetect.com", "India"));

        for (User u : users) {
            // Polymorphic method calls: resolved at runtime based on the actual object type
            System.out.printf("User: %-15s | Role: %-10s | %s%n",
                    u.getUsername(), u.getRole(), u.getDashboardTitle());
        }

        // 2. RUBRIC 1 & 2: Polymorphism & Collections with Transaction hierarchy
        System.out.println("\n--- 2. Polymorphic Transaction List (getCategory & getRiskMultiplier) ---");
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new DomesticTransaction(1, 2, 2500.00, "987654321001",
                "Mumbai", "India", "Grocery shopping"));
        transactions.add(new InternationalTransaction(2, 2, 95000.00, "112233445566",
                "Dubai", "UAE", "Electronics import"));

        for (Transaction t : transactions) {
            // Polymorphic dispatch: getCategory() and getRiskMultiplier()
            System.out.printf("Txn ID: %d | Type: %-13s | Category: %-35s | Multiplier: %.1fx | Amount: ₹%.2f%n",
                    t.getTxnId(), t.getTxnType(), t.getCategory(), t.getRiskMultiplier(), t.getAmount());
        }

        // 3. Risk Level Enum Evaluation
        System.out.println("\n--- 3. RiskLevel.fromScore() Demonstration ---");
        int[] testScores = {15, 55, 88};
        for (int s : testScores) {
            RiskLevel level = RiskLevel.fromScore(s, 40, 70);
            System.out.printf("Score %2d -> Risk Level: %-6s (Display: %s)%n",
                    s, level, level.getDisplayName());
        }

        System.out.println("\n=========================================================================");
        System.out.println(" Phase 2 Verification Completed Successfully!");
        System.out.println("=========================================================================");
    }
}
