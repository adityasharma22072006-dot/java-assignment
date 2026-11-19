import java.sql.*;
import java.util.Scanner;

public class OrderManagementSystem {

    private static final String URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    private static final String USER = "root";
    private static final String PASSWORD = "Aditya@12345"; // <-- change this

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {
            System.out.println("Connected to ecommerce_db successfully.");
            int choice;
            do {
                System.out.println("\n===== ORDER MANAGEMENT SYSTEM =====");
                System.out.println("1. Insert sample orders");
                System.out.println("2. Display orders by status (quantity > 2)");
                System.out.println("3. Increase order quantity");
                System.out.println("4. Update order status");
                System.out.println("5. Total order value / Delete cancelled order");
                System.out.println("6. Exit");
                System.out.print("Enter choice: ");
                choice = Integer.parseInt(sc.nextLine().trim());

                switch (choice) {
                    case 1: insertOrders(con); break;
                    case 2: displayByStatus(con); break;
                    case 3: increaseQuantity(con); break;
                    case 4: updateStatus(con); break;
                    case 5: totalAndDelete(con); break;
                    case 6: System.out.println("Goodbye!"); break;
                    default: System.out.println("Invalid choice.");
                }
            } while (choice != 6);
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }

    // a) Insert records using PreparedStatement, handle duplicate IDs
    static void insertOrders(Connection con) {
        Object[][] data = {
            {101, "Amit Sharma", "Laptop", 1, 55000.0, "Pending"},
            {102, "Neha Verma", "Headphones", 3, 2500.0, "Shipped"},
            {103, "Rahul Singh", "Keyboard", 5, 1200.0, "Delivered"},
            {104, "Priya Gupta", "Mouse", 4, 800.0, "Cancelled"}
        };
        String sql = "INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Object[] row : data) {
                try {
                    ps.setInt(1, (Integer) row[0]);
                    ps.setString(2, (String) row[1]);
                    ps.setString(3, (String) row[2]);
                    ps.setInt(4, (Integer) row[3]);
                    ps.setDouble(5, (Double) row[4]);
                    ps.setString(6, (String) row[5]);
                    ps.executeUpdate();
                    System.out.println("Inserted order " + row[0]);
                } catch (SQLIntegrityConstraintViolationException dup) {
                    System.out.println("Order ID " + row[0] + " already exists. Skipped.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Insert failed: " + e.getMessage());
        }
    }

    // b) Orders by status with quantity > 2, sorted by unit_price DESC
    static void displayByStatus(Connection con) {
        System.out.print("Enter status: ");
        String status = sc.nextLine().trim();
        String sql = "SELECT * FROM Orders WHERE status = ? AND quantity > 2 ORDER BY unit_price DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;
                System.out.printf("%-8s %-15s %-15s %-5s %-10s %-10s%n",
                        "ID", "Customer", "Product", "Qty", "Price", "Status");
                while (rs.next()) {
                    found = true;
                    printRow(rs);
                }
                if (!found) System.out.println("No orders found with status '" + status + "' and quantity > 2.");
            }
        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
        }
    }

    // c) Increase quantity of an existing order
    static void increaseQuantity(Connection con) {
        System.out.print("Enter order ID: ");
        int id = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Enter additional quantity: ");
        int extra = Integer.parseInt(sc.nextLine().trim());

        String sql = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, extra);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("Quantity updated successfully.");
            else System.out.println("Order with ID " + id + " does not exist.");
        } catch (SQLException e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }

    // d) Update status only if order exists and status is valid
    static void updateStatus(Connection con) {
        System.out.print("Enter order ID: ");
        int id = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Enter new status (Pending/Shipped/Delivered): ");
        String newStatus = sc.nextLine().trim();

        if (!(newStatus.equalsIgnoreCase("Pending")
                || newStatus.equalsIgnoreCase("Shipped")
                || newStatus.equalsIgnoreCase("Delivered"))) {
            System.out.println("Invalid status. Allowed: Pending, Shipped, Delivered.");
            return;
        }
        newStatus = newStatus.substring(0, 1).toUpperCase() + newStatus.substring(1).toLowerCase();

        String select = "SELECT * FROM Orders WHERE order_id = ?";
        String update = "UPDATE Orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement sel = con.prepareStatement(select)) {
            sel.setInt(1, id);
            try (ResultSet rs = sel.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Order with ID " + id + " does not exist.");
                    return;
                }
            }
            try (PreparedStatement upd = con.prepareStatement(update)) {
                upd.setString(1, newStatus);
                upd.setInt(2, id);
                upd.executeUpdate();
            }
            try (ResultSet rs = sel.executeQuery()) {
                System.out.println("Updated order details:");
                System.out.printf("%-8s %-15s %-15s %-5s %-10s %-10s%n",
                        "ID", "Customer", "Product", "Qty", "Price", "Status");
                if (rs.next()) printRow(rs);
            }
        } catch (SQLException e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }

    // e) Total order value, then delete only if status is Cancelled
    static void totalAndDelete(Connection con) {
        String sumSql = "SELECT SUM(quantity * unit_price) AS total FROM Orders";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sumSql)) {
            if (rs.next()) System.out.println("Total order value: " + rs.getDouble("total"));
        } catch (SQLException e) {
            System.out.println("Could not compute total: " + e.getMessage());
            return;
        }

        System.out.print("Enter order ID to delete: ");
        int id = Integer.parseInt(sc.nextLine().trim());

        String check = "SELECT status FROM Orders WHERE order_id = ?";
        String delete = "DELETE FROM Orders WHERE order_id = ?";
        try (PreparedStatement ps = con.prepareStatement(check)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Order with ID " + id + " does not exist.");
                    return;
                }
                if (!rs.getString("status").equalsIgnoreCase("Cancelled")) {
                    System.out.println("Order cannot be deleted. Only 'Cancelled' orders can be deleted.");
                    return;
                }
            }
            try (PreparedStatement del = con.prepareStatement(delete)) {
                del.setInt(1, id);
                del.executeUpdate();
                System.out.println("Cancelled order " + id + " deleted successfully.");
            }
        } catch (SQLException e) {
            System.out.println("Delete failed: " + e.getMessage());
        }
    }

    static void printRow(ResultSet rs) throws SQLException {
        System.out.printf("%-8d %-15s %-15s %-5d %-10.2f %-10s%n",
                rs.getInt("order_id"), rs.getString("customer_name"),
                rs.getString("product_name"), rs.getInt("quantity"),
                rs.getDouble("unit_price"), rs.getString("status"));
    }
}