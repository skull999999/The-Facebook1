package thefacebook.ui;

import thefacebook.datastructures.BrowsingHistory;
import thefacebook.model.FriendRequest;
import thefacebook.model.Role;
import thefacebook.model.User;
import thefacebook.service.SocialNetwork;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    private final SocialNetwork network;
    private final Scanner scanner = new Scanner(System.in);
    private final BrowsingHistory history = new BrowsingHistory();

    public ConsoleUI(SocialNetwork network) {
        this.network = network;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== TheFacebook 2004 ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Search users");
            System.out.println("4. Exit");
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1:
                    login();
                    break;
                case 2:
                    register();
                    break;
                case 3:
                    searchUsers(null);
                    break;
                case 4:
                    network.saveAll();
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
        System.out.println("Goodbye from TheFacebook.");
    }

    private void login() {
        String emailOrPhone = readText("Email or phone: ");
        String password = readText("Password: ");
        User user = network.login(emailOrPhone, password);
        if (user == null) {
            System.out.println("Login failed.");
            return;
        }
        history.clear();
        history.visit("Logged in as " + user.getUsername());
        userMenu(user);
    }

    private void register() {
        System.out.println("\n--- Register Account ---");
        String name = readText("Name: ");
        String username = readText("Username: ");
        String email = readText("Email: ");
        String phone = readText("Phone: ");
        String password = readText("Password: ");
        String confirm = readText("Retype password: ");
        System.out.println(network.register(name, username, email, phone, password, confirm));
    }

    private void userMenu(User current) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n=== Home: " + current.getName() + " (" + current.getRole() + ") ===");
            System.out.println("1. View my account");
            System.out.println("2. Edit my account");
            System.out.println("3. Search and view users");
            System.out.println("4. Friend recommendations");
            System.out.println("5. Friend requests");
            System.out.println("6. My friends");
            System.out.println("7. Mutual friends");
            System.out.println("8. Traceback history");
            System.out.println("9. Generate data analysis report");
            if (current.getRole() == Role.ADMIN) {
                System.out.println("10. Admin: delete user");
                System.out.println("11. Logout");
            } else {
                System.out.println("10. Logout");
            }
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1:
                    viewProfile(current, current);
                    break;
                case 2:
                    editProfile(current);
                    break;
                case 3:
                    searchUsers(current);
                    break;
                case 4:
                    recommendations(current);
                    break;
                case 5:
                    manageRequests(current);
                    break;
                case 6:
                    listFriends(current);
                    break;
                case 7:
                    mutualFriends();
                    break;
                case 8:
                    System.out.println(history.back());
                    break;
                case 9:
                    System.out.println(network.generateAnalysisReport());
                    break;
                case 10:
                    if (current.getRole() == Role.ADMIN) {
                        deleteUser(current);
                    } else {
                        loggedIn = false;
                    }
                    break;
                case 11:
                    if (current.getRole() == Role.ADMIN) {
                        loggedIn = false;
                    } else {
                        System.out.println("Invalid choice.");
                    }
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
        history.clear();
    }

    private void viewProfile(User viewer, User target) {
        history.visit("Viewed profile: " + target.getUsername());
        int degree = network.connectionDegree(viewer.getId(), target.getId());
        System.out.println("\n--- Profile ---");
        System.out.println(target.publicProfile(network.friendCount(target.getId())));
        if (viewer.getId().equals(target.getId())) {
            System.out.println("Connection: This is your account.");
        } else if (degree == -1) {
            System.out.println("Connection: Not connected within 3 degrees.");
        } else {
            System.out.println("Connection: " + degree + ordinalSuffix(degree) + " degree.");
        }
    }

    private void editProfile(User user) {
        history.visit("Edited profile");
        boolean editing = true;
        while (editing) {
            System.out.println("\n--- Edit Account ---");
            System.out.println("1. Name");
            System.out.println("2. Username");
            System.out.println("3. Email");
            System.out.println("4. Phone");
            System.out.println("5. Birthday");
            System.out.println("6. Address");
            System.out.println("7. Gender");
            System.out.println("8. Relationship status");
            System.out.println("9. Hobbies");
            System.out.println("10. Career history");
            System.out.println("11. Back");
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1:
                    user.setName(readText("New name: "));
                    break;
                case 2:
                    user.setUsername(readText("New username: "));
                    break;
                case 3:
                    user.setEmail(readText("New email: "));
                    break;
                case 4:
                    user.setPhone(readText("New phone: "));
                    break;
                case 5:
                    updateBirthday(user);
                    break;
                case 6:
                    user.setAddress(readText("New address: "));
                    break;
                case 7:
                    chooseGender(user);
                    break;
                case 8:
                    chooseRelationship(user);
                    break;
                case 9:
                    editHobbies(user);
                    break;
                case 10:
                    editCareer(user);
                    break;
                case 11:
                    editing = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
            network.updateUser(user);
        }
    }

    private void updateBirthday(User user) {
        try {
            user.setBirthday(LocalDate.parse(readText("Birthday (YYYY-MM-DD): ")));
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format.");
        }
    }

    private void chooseGender(User user) {
        ArrayList<String> options = new ArrayList<>(Arrays.asList("Female", "Male", "Other", "Prefer not to say"));
        chooseFromArrayList("Gender", options, user::setGender);
    }

    private void chooseRelationship(User user) {
        ArrayList<String> options = new ArrayList<>(Arrays.asList("Single", "In a relationship", "It's complicated", "Married"));
        chooseFromArrayList("Relationship", options, user::setRelationshipStatus);
    }

    private void editHobbies(User user) {
        System.out.println("Current hobbies: " + user.getHobbies());
        ArrayList<String> options = new ArrayList<>(Arrays.asList("Coding", "Music", "Basketball", "Reading", "Gaming", "Art"));
        System.out.println("Suggested hobbies stored in ArrayList:");
        printNumbered(options);
        String hobby = readText("Type a hobby to add, or leave blank to stop: ");
        if (!hobby.trim().isEmpty()) {
            user.getHobbies().add(hobby);
        }
    }

    private void editCareer(User user) {
        System.out.println("Career stack, latest shown first: " + user.getCareerHistory());
        String career = readText("Push latest job/school experience, or leave blank to stop: ");
        if (!career.trim().isEmpty()) {
            user.getCareerHistory().push(career);
        }
    }

    private void searchUsers(User current) {
        history.visit("Searched users");
        String keyword = readText("Search by name, username, ID, email, or phone: ");
        ArrayList<User> results = network.searchUsers(keyword);
        if (results.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        printUsers(results);
        if (current == null) {
            return;
        }
        int choice = readInt("Open result number, or 0 to cancel: ");
        if (choice > 0 && choice <= results.size()) {
            User target = results.get(choice - 1);
            viewProfile(current, target);
            if (!current.getId().equals(target.getId())) {
                String add = readText("Send friend request? (y/n): ");
                if (add.equalsIgnoreCase("y")) {
                    System.out.println(network.sendFriendRequest(current.getId(), target.getId()));
                }
            }
        }
    }

    private void recommendations(User current) {
        history.visit("Viewed friend recommendations");
        ArrayList<User> users = network.friendRecommendations(current.getId());
        if (users.isEmpty()) {
            System.out.println("No recommendations yet.");
            return;
        }
        System.out.println("\n--- Friend Recommendations ---");
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            int degree = network.connectionDegree(current.getId(), user.getId());
            int mutuals = network.commonFriends(current.getId(), user.getId()).size();
            System.out.println((i + 1) + ". " + user.getName() + " [" + user.getId() + "] - "
                    + degree + ordinalSuffix(degree) + " degree, " + mutuals + " mutual friend(s)");
        }
        int choice = readInt("Send request to number, or 0 to cancel: ");
        if (choice > 0 && choice <= users.size()) {
            System.out.println(network.sendFriendRequest(current.getId(), users.get(choice - 1).getId()));
        }
    }

    private void manageRequests(User current) {
        history.visit("Reviewed friend requests");
        List<FriendRequest> requests = network.requestsFor(current.getId());
        if (requests.isEmpty()) {
            System.out.println("No incoming requests.");
            return;
        }
        for (int i = 0; i < requests.size(); i++) {
            FriendRequest request = requests.get(i);
            User from = network.getUser(request.getFromUserId());
            System.out.println((i + 1) + ". " + (from == null ? request.getFromUserId() : from.getName())
                    + " [" + request.getFromUserId() + "]");
        }
        int choice = readInt("Choose request, or 0 to cancel: ");
        if (choice <= 0 || choice > requests.size()) {
            return;
        }
        String answer = readText("Accept? (y/n): ");
        FriendRequest request = requests.get(choice - 1);
        System.out.println(network.respondToRequest(current.getId(), request.getFromUserId(), answer.equalsIgnoreCase("y")));
    }

    private void listFriends(User current) {
        history.visit("Viewed friend list");
        ArrayList<User> friends = network.friendsOf(current.getId());
        if (friends.isEmpty()) {
            System.out.println("You have no friends yet.");
            return;
        }
        printUsers(friends);
    }

    private void mutualFriends() {
        history.visit("Checked mutual friends");
        String first = readText("First user ID: ");
        String second = readText("Second user ID: ");
        ArrayList<User> mutuals = network.commonFriends(first, second);
        if (mutuals.isEmpty()) {
            System.out.println("No mutual friends found.");
            return;
        }
        System.out.println("--- Mutual Friends ---");
        printUsers(mutuals);
    }

    private void deleteUser(User admin) {
        String targetId = readText("Target user ID to delete: ");
        System.out.println(network.deleteUser(admin, targetId));
    }

    private interface SelectionHandler {
        void accept(String value);
    }

    private void chooseFromArrayList(String label, ArrayList<String> options, SelectionHandler handler) {
        System.out.println(label + " options stored in ArrayList:");
        printNumbered(options);
        int choice = readInt("Choose: ");
        if (choice > 0 && choice <= options.size()) {
            handler.accept(options.get(choice - 1));
        } else {
            System.out.println("Invalid option.");
        }
    }

    private void printUsers(ArrayList<User> users) {
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            System.out.println((i + 1) + ". " + user.getName() + " | " + user.getUsername()
                    + " | " + user.getId() + " | " + user.getEmail());
        }
    }

    private void printNumbered(List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            System.out.println((i + 1) + ". " + values.get(i));
        }
    }

    private String readText(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int readInt(String prompt) {
        while (true) {
            String text = readText(prompt);
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private String ordinalSuffix(int value) {
        if (value == 1) {
            return "st";
        }
        if (value == 2) {
            return "nd";
        }
        if (value == 3) {
            return "rd";
        }
        return "th";
    }
}
