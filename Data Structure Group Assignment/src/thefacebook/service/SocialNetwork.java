package thefacebook.service;

import thefacebook.datastructures.FriendGraph;
import thefacebook.datastructures.MyHashTable;
import thefacebook.model.FriendRequest;
import thefacebook.model.Role;
import thefacebook.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class SocialNetwork {
    private final CsvStorage storage;
    private final MyHashTable<String, User> usersById = new MyHashTable<>();
    private final MyHashTable<String, User> usersByEmail = new MyHashTable<>();
    private final MyHashTable<String, User> usersByPhone = new MyHashTable<>();
    private final MyHashTable<String, User> usersByUsername = new MyHashTable<>();
    private final FriendGraph graph = new FriendGraph();
    // Queue is used because friend requests should be reviewed in first-in-first-out order.
    private final Queue<FriendRequest> requests = new LinkedList<>();

    public SocialNetwork(String dataFolder) {
        this.storage = new CsvStorage(dataFolder);
    }

    public void load() {
        storage.ensureFolder();
        List<User> loaded = storage.loadUsers();
        if (loaded.isEmpty()) {
            seedData();
            saveAll();
            return;
        }
        for (User user : loaded) {
            addToIndexes(user);
        }
        for (String[] row : storage.loadFriendships()) {
            if (row.length >= 2) {
                graph.addFriendship(row[0], row[1]);
            }
        }
        requests.addAll(storage.loadRequests());
    }

    public String register(String name, String username, String email, String phone, String password, String confirm) {
        if (!password.equals(confirm)) {
            return "Passwords do not match.";
        }
        if (!email.contains("@") || phone.length() < 8) {
            return "Email or phone format is invalid.";
        }
        if (usersByEmail.containsKey(email) || usersByPhone.containsKey(phone) || usersByUsername.containsKey(username)) {
            return "Username, email, or phone already exists.";
        }
        String id = "U" + (usersById.values().size() + 1001);
        User user = new User(id, name, username, email, phone, PasswordUtil.sha256(password),
                LocalDate.of(2004, 1, 1), "Not set", "Not set", "Single", Role.USER);
        addToIndexes(user);
        saveAll();
        return "Registered successfully. Your ID is " + id;
    }

    public User login(String emailOrPhone, String password) {
        User user = usersByEmail.get(emailOrPhone);
        if (user == null) {
            user = usersByPhone.get(emailOrPhone);
        }
        if (user != null && user.getPasswordHash().equals(PasswordUtil.sha256(password))) {
            return user;
        }
        return null;
    }

    public void updateUser(User user) {
        rebuildIndexes();
        saveAll();
    }

    public String sendFriendRequest(String fromId, String toId) {
        if (fromId.equals(toId)) {
            return "You cannot add yourself.";
        }
        if (usersById.get(toId) == null) {
            return "Target user does not exist.";
        }
        if (graph.areFriends(fromId, toId)) {
            return "You are already friends.";
        }
        for (FriendRequest request : requests) {
            if (request.getFromUserId().equals(fromId) && request.getToUserId().equals(toId)) {
                return "Friend request already sent.";
            }
        }
        requests.add(new FriendRequest(fromId, toId));
        saveAll();
        return "Friend request sent.";
    }

    public List<FriendRequest> requestsFor(String userId) {
        ArrayList<FriendRequest> result = new ArrayList<>();
        for (FriendRequest request : requests) {
            if (request.getToUserId().equals(userId)) {
                result.add(request);
            }
        }
        return result;
    }

    public String respondToRequest(String currentUserId, String fromUserId, boolean accept) {
        Queue<FriendRequest> kept = new LinkedList<>();
        boolean found = false;
        while (!requests.isEmpty()) {
            FriendRequest request = requests.poll();
            if (request.getFromUserId().equals(fromUserId) && request.getToUserId().equals(currentUserId)) {
                found = true;
                if (accept) {
                    graph.addFriendship(fromUserId, currentUserId);
                }
            } else {
                kept.add(request);
            }
        }
        requests.addAll(kept);
        saveAll();
        if (!found) {
            return "Request not found.";
        }
        return accept ? "Friend request accepted." : "Friend request rejected.";
    }

    public ArrayList<User> searchUsers(String keyword) {
        String lower = keyword.toLowerCase();
        ArrayList<User> result = new ArrayList<>();
        for (User user : usersById.values()) {
            if (contains(user.getName(), lower) || contains(user.getUsername(), lower) || contains(user.getId(), lower)
                    || contains(user.getEmail(), lower) || contains(user.getPhone(), lower)) {
                if (!result.contains(user)) {
                    result.add(user);
                }
            }
        }
        sortByName(result);
        return result;
    }

    public ArrayList<User> commonFriends(String a, String b) {
        ArrayList<User> result = new ArrayList<>();
        for (String id : graph.commonFriends(a, b)) {
            User user = usersById.get(id);
            if (user != null) {
                result.add(user);
            }
        }
        sortByName(result);
        return result;
    }

    public ArrayList<User> friendRecommendations(String userId) {
        ArrayList<User> result = new ArrayList<>();
        for (String id : graph.connectionRecommendations(userId)) {
            User user = usersById.get(id);
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }

    public int connectionDegree(String fromUserId, String toUserId) {
        return graph.connectionDegree(fromUserId, toUserId);
    }

    public String deleteUser(User actor, String targetId) {
        if (actor.getRole() != Role.ADMIN) {
            return "Only admins can delete users.";
        }
        User target = usersById.get(targetId);
        if (target == null) {
            return "User not found.";
        }
        usersById.remove(targetId);
        graph.removeUser(targetId);
        rebuildIndexes();
        saveAll();
        return "User deleted: " + target.getName();
    }

    public User getUser(String id) {
        return usersById.get(id);
    }

    public ArrayList<User> friendsOf(String userId) {
        ArrayList<User> result = new ArrayList<>();
        for (String id : graph.friendsOf(userId)) {
            User user = usersById.get(id);
            if (user != null) {
                result.add(user);
            }
        }
        sortByName(result);
        return result;
    }

    public int friendCount(String userId) {
        return graph.friendsOf(userId).size();
    }

    public String generateAnalysisReport() {
        ArrayList<String> lines = new ArrayList<>();
        int users = usersById.values().size();
        int admins = 0;
        int normalUsers = 0;
        int male = 0;
        int female = 0;
        int otherGender = 0;
        int totalAge = 0;
        int friendshipEdges = 0;

        for (User user : usersById.values()) {
            if (user.getRole() == Role.ADMIN) {
                admins++;
            } else {
                normalUsers++;
            }
            if ("Male".equalsIgnoreCase(user.getGender())) {
                male++;
            } else if ("Female".equalsIgnoreCase(user.getGender())) {
                female++;
            } else {
                otherGender++;
            }
            totalAge += user.getAge();
            friendshipEdges += graph.friendsOf(user.getId()).size();
        }

        int averageAge = users == 0 ? 0 : totalAge / users;
        int friendships = friendshipEdges / 2;
        lines.add("TheFacebook Data Analysis Report");
        lines.add("Generated date: " + LocalDate.now());
        lines.add("Total accounts: " + users);
        lines.add("Normal users: " + normalUsers);
        lines.add("Admins: " + admins);
        lines.add("Friendship connections: " + friendships);
        lines.add("Pending friend requests: " + requests.size());
        lines.add("Average age: " + averageAge);
        lines.add("Gender - Male: " + male);
        lines.add("Gender - Female: " + female);
        lines.add("Gender - Other/Not set: " + otherGender);
        lines.add("Actionable insight: If pending requests are high, users may need clearer request notifications.");

        return "Report generated at " + storage.writeReport("analysis_report.txt", lines).toAbsolutePath();
    }

    public void saveAll() {
        storage.saveUsers(usersById.values());
        ArrayList<String> friendshipRows = new ArrayList<>();
        for (String a : graph.allUserIds()) {
            for (String b : graph.friendsOf(a)) {
                if (a.compareTo(b) < 0) {
                    friendshipRows.add(a + "|" + b);
                }
            }
        }
        storage.saveFriendships(friendshipRows);
        storage.saveRequests(new ArrayList<>(requests));
    }

    private boolean contains(String value, String lower) {
        return value != null && value.toLowerCase().contains(lower);
    }

    private void addToIndexes(User user) {
        usersById.put(user.getId(), user);
        usersByEmail.put(user.getEmail(), user);
        usersByPhone.put(user.getPhone(), user);
        usersByUsername.put(user.getUsername(), user);
        graph.addUser(user.getId());
    }

    private void rebuildIndexes() {
        List<User> users = usersById.values();
        usersByEmail.clear();
        usersByPhone.clear();
        usersByUsername.clear();
        for (User user : users) {
            usersByEmail.put(user.getEmail(), user);
            usersByPhone.put(user.getPhone(), user);
            usersByUsername.put(user.getUsername(), user);
        }
    }

    private void sortByName(ArrayList<User> users) {
        for (int i = 0; i < users.size() - 1; i++) {
            for (int j = 0; j < users.size() - i - 1; j++) {
                if (users.get(j).getName().compareToIgnoreCase(users.get(j + 1).getName()) > 0) {
                    User temp = users.get(j);
                    users.set(j, users.get(j + 1));
                    users.set(j + 1, temp);
                }
            }
        }
    }

    private void seedData() {
        for (int i = 1; i <= 32; i++) {
            Role role = i <= 2 ? Role.ADMIN : Role.USER;
            String id = role == Role.ADMIN ? "A" + i : "U" + i;
            User user = new User(id, "Clarice User" + i, "user" + i, "user" + i + "@thefacebook.edu",
                    "6012000" + String.format("%03d", i), PasswordUtil.sha256("pass" + i),
                    LocalDate.of(1980 + (i % 20), (i % 12) + 1, (i % 25) + 1),
                    "Harvard House " + ((i % 5) + 1), i % 2 == 0 ? "Female" : "Male",
                    i % 3 == 0 ? "In a relationship" : "Single", role);
            user.getHobbies().add("Coding");
            user.getHobbies().add(i % 2 == 0 ? "Music" : "Basketball");
            user.getCareerHistory().push("Student");
            addToIndexes(user);
        }
        for (int i = 3; i <= 28; i++) {
            graph.addFriendship("U" + i, "U" + (i + 1));
            if (i + 2 <= 32) {
                graph.addFriendship("U" + i, "U" + (i + 2));
            }
        }
        graph.addFriendship("A1", "U3");
        graph.addFriendship("A2", "U4");
        requests.add(new FriendRequest("U5", "U10"));
        requests.add(new FriendRequest("U8", "U10"));
    }
}
