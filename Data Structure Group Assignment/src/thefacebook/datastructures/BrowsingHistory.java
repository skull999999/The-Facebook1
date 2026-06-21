package thefacebook.datastructures;

public class BrowsingHistory {
    private static class Node {
        String action;
        Node previous;
        Node next;

        Node(String action) {
            this.action = action;
        }
    }

    private Node current;

    public void visit(String action) {
        Node node = new Node(action);
        if (current != null) {
            current.next = node;
            node.previous = current;
        }
        current = node;
    }

    public String back() {
        if (current == null) {
            return "No browsing history in this login session.";
        }
        String leaving = current.action;
        current = current.previous;
        if (current == null) {
            return "Back from: " + leaving + "\nNow at login menu.";
        }
        return "Back from: " + leaving + "\nNow at: " + current.action;
    }

    public void clear() {
        current = null;
    }
}
