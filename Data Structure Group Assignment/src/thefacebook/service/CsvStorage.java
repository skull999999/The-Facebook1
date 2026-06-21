package thefacebook.service;

import thefacebook.model.FriendRequest;
import thefacebook.model.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CsvStorage {
    private final Path folder;

    public CsvStorage(String folder) {
        this.folder = Paths.get(folder);
    }

    public void ensureFolder() {
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create data folder", e);
        }
    }

    public List<User> loadUsers() {
        Path file = folder.resolve("users.csv");
        ArrayList<User> users = new ArrayList<>();
        if (!Files.exists(file)) {
            return users;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.trim().isEmpty()) {
                    users.add(User.fromCsv(line));
                }
            }
            return users;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read users.csv", e);
        }
    }

    public void saveUsers(List<User> users) {
        ArrayList<String> lines = new ArrayList<>();
        for (User user : users) {
            lines.add(user.toCsv());
        }
        write("users.csv", lines);
    }

    public List<String[]> loadFriendships() {
        Path file = folder.resolve("friendships.csv");
        ArrayList<String[]> rows = new ArrayList<>();
        if (!Files.exists(file)) {
            return rows;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.trim().isEmpty()) {
                    rows.add(line.split("\\|", -1));
                }
            }
            return rows;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read friendships.csv", e);
        }
    }

    public void saveFriendships(List<String> rows) {
        write("friendships.csv", rows);
    }

    public List<FriendRequest> loadRequests() {
        Path file = folder.resolve("requests.csv");
        ArrayList<FriendRequest> requests = new ArrayList<>();
        if (!Files.exists(file)) {
            return requests;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.trim().isEmpty()) {
                    requests.add(FriendRequest.fromCsv(line));
                }
            }
            return requests;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read requests.csv", e);
        }
    }

    public void saveRequests(List<FriendRequest> requests) {
        ArrayList<String> lines = new ArrayList<>();
        for (FriendRequest request : requests) {
            lines.add(request.toCsv());
        }
        write("requests.csv", lines);
    }

    public Path writeReport(String fileName, List<String> lines) {
        write(fileName, lines);
        return folder.resolve(fileName);
    }

    private void write(String name, List<String> lines) {
        try {
            Files.write(folder.resolve(name), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write " + name, e);
        }
    }
}
