import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.io.*;

public class EscapeRoomGame {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        GameEngine engine = new GameEngine(scanner);

        AuthSystem auth = new AuthSystem(scanner, engine);
        User currentUser = null;

        engine.displayTitle();

        while (true) {
            
            while (currentUser == null) {
                String authMenu = "1. Login\n" +
                "2. Signup\n" +
                "3. Exit";
                
                engine.drawBox("MENU", authMenu, engine.CYAN, false);
                System.out.println(engine.GRAY + engine.centerText("Type 'back' to return", engine.UI_WIDTH) + engine.RESET);
                
                String choice = scanner.nextLine();

                if (choice.equals("1")) {
                    currentUser = auth.loginMenu();
                } else if (choice.equals("2")) {
                    auth.signup();
                } else if (choice.equals("3")) {
                    System.out.println("\u001b[33m" + engine.centerText("EXITING...", engine.UI_WIDTH) + "\u001b[0m");
                    return;
                } else {
                    System.out
                            .println("\u001b[31m" + engine.centerText("INVALID CHOICE", engine.UI_WIDTH) + "\u001b[0m");
                }
            }

            while (currentUser != null) {

                String dashboardMenu = "1. Play Game\n" +
                        "2. View Score History\n" +
                        "3. Logout";

                engine.drawBox("DASHBOARD", dashboardMenu, engine.PURPLE, false);

                String choice = scanner.nextLine();

                if (choice.equals("1")) {
                    engine.startGame(currentUser);

                    int finalScore = engine.getPlayerScore();
                    currentUser.addScore(finalScore, engine);

                    auth.save();

                } else if (choice.equals("2")) {
                    List<Integer> scores = currentUser.getScores();

                    String history;

                    if (scores.isEmpty()) {
                        history = "No scores yet.";
                    } else {
                        history = "Score History: " + scores + "\n" +
                                "Highest Score: " + Collections.max(scores);
                    }

                    engine.drawBox("SCORE HISTORY", history, engine.GREEN, false);

                } else if (choice.equals("3")) {
                    System.out
                            .println("\u001b[33m" + engine.centerText("LOGGING OUT...", engine.UI_WIDTH) + "\u001b[0m");
                    currentUser = null;
                    break;
                } else {
                    System.out
                            .println("\u001b[31m" + engine.centerText("INVALID CHOICE", engine.UI_WIDTH) + "\u001b[0m");
                }
            }
        }
    }
}

class User {

    private final String name;
    private final String email;
    private String password;
    private final String mobile;
    private boolean isNewUser;

    private List<Integer> scores;
    private int highScore;


    public User(String name, String email, String password, String mobile) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.mobile = mobile;
        this.isNewUser = true;
        this.scores = new ArrayList<>();
        this.highScore = 0;
    }


    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getMobile() {
        return mobile;
    }

    public boolean isNewUser() {
        return isNewUser;
    }

    public List<Integer> getScores() {
        return new ArrayList<>(scores);
    }

    public int getHighScore() {
        return highScore;
    }


    public void setPassword(String password) {
        if (password != null && !password.trim().isEmpty()) {
            this.password = password;
        }
    }

    public void setNewUser(boolean status) {
        this.isNewUser = status;
    }

    public void setScores(List<Integer> scores) {
        this.scores = new ArrayList<>();

        if (scores != null) {
            for (int s : scores) {
                if (s >= 0) {
                    this.scores.add(s);
                }
            }
        }

        recalculateHighScore();
    }


    public void addScore(int score, GameEngine engine) {

        if (score < 0)
            return;

        scores.add(score);

        if (score > highScore) {
            highScore = score;

            System.out.println(
                    engine.RED +
                            engine.centerText("NEW HIGH SCORE: " + score, engine.UI_WIDTH) +
                            engine.RESET);
        }
    }

    private void recalculateHighScore() {
        highScore = 0;
        for (int s : scores) {
            if (s > highScore) {
                highScore = s;
            }
        }
    }


    public void showHistory(GameEngine engine) {

        System.out.println(
                engine.BLUE +
                        engine.centerText("SCORE HISTORY", engine.UI_WIDTH) +
                        engine.RESET);

        System.out.println(
                engine.YELLOW +
                        engine.centerText("HIGHEST SCORE: " + highScore, engine.UI_WIDTH) +
                        engine.RESET);

        if (scores.isEmpty()) {
            System.out.println(
                    engine.GRAY +
                            engine.centerText("No scores yet.", engine.UI_WIDTH) +
                            engine.RESET);
            return;
        }

        int index = 1;
        for (int s : scores) {
            System.out.println(
                    engine.WHITE +
                            engine.centerText("[" + index + "] Score: " + s, engine.UI_WIDTH) +
                            engine.RESET);
            index++;
        }
    }
}

class UserStore {

    private static final String FILE_NAME = "users.txt";


    public List<User> loadUsers(GameEngine engine) {

        List<User> users = new ArrayList<>();
        File file = new File(FILE_NAME);

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            System.out.println(
                    engine.RED +
                    engine.centerText("Error initializing user storage", engine.UI_WIDTH) +
                    engine.RESET
            );
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] data = line.split("::");  

                if (data.length < 5) continue;   

                User user = new User(
                        data[0].trim(),
                        data[1].trim(),
                        data[2].trim(),
                        data[3].trim()
                );

                user.setNewUser(Boolean.parseBoolean(data[4].trim()));

                if (data.length >= 6 && !data[5].trim().isEmpty()) {

                    String[] scoreParts = data[5].split("\\|");
                    List<Integer> scores = new ArrayList<>();

                    for (String s : scoreParts) {
                        try {
                            scores.add(Integer.parseInt(s.trim()));
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    user.setScores(scores);
                }

                users.add(user);
            }

        } catch (IOException e) {
            System.out.println(
                    engine.RED +
                    engine.centerText("Error reading user data", engine.UI_WIDTH) +
                    engine.RESET
            );
        }

        return users;
    }


    public void saveUsers(List<User> users, GameEngine engine) {

        File file = new File(FILE_NAME);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {

            for (User user : users) {

                StringBuilder scoresBuilder = new StringBuilder();
                List<Integer> scores = user.getScores();

                for (int i = 0; i < scores.size(); i++) {
                    scoresBuilder.append(scores.get(i));
                    if (i != scores.size() - 1) {
                        scoresBuilder.append("|");
                    }
                }

                String line = user.getName() + "::" +
                        user.getEmail() + "::" +
                        user.getPassword() + "::" +
                        user.getMobile() + "::" +
                        user.isNewUser() + "::" +
                        scoresBuilder;

                bw.write(line);
                bw.newLine();
            }

        } catch (IOException e) {
            System.out.println(
                    engine.RED +
                    engine.centerText("Error saving user data", engine.UI_WIDTH) +
                    engine.RESET
            );
        }
    }
}

class Validator {


    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z ]{3,}$");

    private static final Pattern EMAIL_PATTERN = Pattern
            .compile("^[A-Za-z][A-Za-z0-9._]*@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern MOBILE_PATTERN_10 = Pattern.compile("[6-9]\\d{9}");

    private static final Pattern MOBILE_PATTERN_11 = Pattern.compile("0\\d{10}");

    private static final Pattern UPPER_PATTERN = Pattern.compile(".*[A-Z].*");

    private static final Pattern LOWER_PATTERN = Pattern.compile(".*[a-z].*");

    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");

    private static final Pattern SPECIAL_PATTERN = Pattern.compile(".*[@#$%&*!].*");


    public static boolean isValidName(String name) {
        if (name == null)
            return false;

        name = name.trim();

        if (name.isEmpty())
            return false;

        return NAME_PATTERN.matcher(name).matches();
    }


    public static boolean isValidEmail(String email) {
        if (email == null)
            return false;

        email = email.trim();

        if (email.isEmpty())
            return false;

        if (email.length() < 5)
            return false;

        if (Character.isDigit(email.charAt(0)))
            return false;

        if (email.indexOf('@') != email.lastIndexOf('@'))
            return false;

        return EMAIL_PATTERN.matcher(email).matches();
    }


    public static boolean isValidMobile(String mobile) {
        if (mobile == null)
            return false;

        mobile = mobile.trim();

        if (mobile.isEmpty())
            return false;

        return MOBILE_PATTERN_10.matcher(mobile).matches()
                || MOBILE_PATTERN_11.matcher(mobile).matches();
    }


    public static boolean isValidPassword(String password, String username) {
        if (password == null)
            return false;

        password = password.trim();

        if (password.isEmpty())
            return false;

        if (password.length() < 8 || password.length() > 20)
            return false;

        if (password.contains(" "))
            return false;

        if (username != null && password.equalsIgnoreCase(username.trim()))
            return false;

        boolean hasUpper = UPPER_PATTERN.matcher(password).matches();
        boolean hasLower = LOWER_PATTERN.matcher(password).matches();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).matches();
        boolean hasSpecial = SPECIAL_PATTERN.matcher(password).matches();

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}

class AuthSystem {

    private List<User> users;
    private Scanner scanner;
    private UserStore store;
    GameEngine engine;

    public AuthSystem(Scanner scanner, GameEngine engine) {
        this.scanner = scanner;
        this.engine = engine;
        this.store = new UserStore();
        this.users = store.loadUsers(engine);
    }

    public void save() {
        store.saveUsers(users, engine);
    }

    private String hashPassword(String password) {
    return Integer.toHexString(password.hashCode());
}


    public void signup() {

        if (users.size() >= 10) {
            System.out
                    .println(engine.RED + engine.centerText("USER LIMIT REACHED (10)", engine.UI_WIDTH) + engine.RESET);
            return;
        }

        String name;
        while (true) {
            System.out.println(engine.WHITE + engine.centerText("ENTER FULL NAME: ", engine.UI_WIDTH) + engine.RESET);
            name = scanner.nextLine().trim();

            if (name.equalsIgnoreCase("back"))
                return;

            if (!Validator.isValidName(name)) {
                System.out.println(engine.RED + engine.centerText(
                        "Only letters are allowed, minimum 3 characters",
                        engine.UI_WIDTH) + engine.RESET);
            } else
                break;
        }

        String email;
        while (true) {
            System.out.println(engine.WHITE + engine.centerText("ENTER EMAIL: ", engine.UI_WIDTH) + engine.RESET);
            email = scanner.nextLine().trim().toLowerCase();

            if (email.equalsIgnoreCase("back"))
                return;

            if (!Validator.isValidEmail(email)) {
                System.out.println(engine.RED + engine.centerText(
                        "Invalid format : example@domain.com",
                        engine.UI_WIDTH) + engine.RESET);
            } else if (findByEmail(email) != null) {
                System.out.println(
                        engine.RED + engine.centerText("Email already exists", engine.UI_WIDTH) + engine.RESET);
            } else
                break;
        }

        String mobile;
        while (true) {
            System.out.println(engine.WHITE + engine.centerText("ENTER MOBILE: ", engine.UI_WIDTH) + engine.RESET);
            mobile = scanner.nextLine().trim();

            if (mobile.equalsIgnoreCase("back"))
                return;

            if (!Validator.isValidMobile(mobile)) {
                System.out.println(engine.RED + engine.centerText(
                        "Must be 10 digits (start with 6-9) OR 0 + 10 digits",
                        engine.UI_WIDTH) + engine.RESET);
            } else if (findByMobile(mobile) != null) {
                System.out
                        .println(engine.RED + engine.centerText("Already Registered", engine.UI_WIDTH) + engine.RESET);
            } else
                break;
        }

        String password;
        while (true) {
            System.out.println(engine.WHITE + engine.centerText("ENTER PASSWORD: ", engine.UI_WIDTH) + engine.RESET);
            password = scanner.nextLine();

            if (password.equalsIgnoreCase("back"))
                return;

            if (!Validator.isValidPassword(password, name)) {
                System.out.println(engine.RED + engine.centerText(
                        "Minimum 8 characters (1 Upper letter, 1 Lower letter, 1 Special character, 1 Digit)",
                        engine.UI_WIDTH) + engine.RESET);
            } else
                break;
        }

        while (true) {
            System.out.println(engine.WHITE + engine.centerText("CONFIRM PASSWORD: ", engine.UI_WIDTH) + engine.RESET);
            String confirm = scanner.nextLine();

            if (confirm.equalsIgnoreCase("back"))
                return;

            if (password.equals(confirm))
                break;

            System.out.println(engine.RED + engine.centerText("Mismatch, try again", engine.UI_WIDTH) + engine.RESET);
        }

        int otp = (int) (Math.random() * 9000) + 1000;

        System.out
                .println(engine.WHITE + engine.centerText("OTP SENT TO EMAIL: " + otp, engine.UI_WIDTH) + engine.RESET);

        int attempts = 0;
        boolean verified = false;

        while (attempts < 3) {

            System.out.println(engine.WHITE + engine.centerText("ENTER OTP: ", engine.UI_WIDTH) + engine.RESET);
            String enteredOtp = scanner.nextLine();

            if (enteredOtp.equalsIgnoreCase("back"))
                return;

            if (enteredOtp.equals(String.valueOf(otp))) {
                verified = true;
                break;
            }

            attempts++;
            System.out.println(engine.RED + engine.centerText(
                    "Invalid OTP (" + attempts + "/3)", engine.UI_WIDTH) + engine.RESET);
        }

        if (!verified) {
            System.out
                    .println(engine.RED + engine.centerText("OTP VERIFICATION FAILED", engine.UI_WIDTH) + engine.RESET);
            return;
        }

        String hashedPassword = hashPassword(password);
User newUser = new User(name, email, hashedPassword, mobile);
        users.add(newUser);

        store.saveUsers(users, engine);

        System.out.println(engine.GREEN + engine.centerText("SIGNUP SUCCESSFUL", engine.UI_WIDTH) + engine.RESET);
    }


    public User loginMenu() {

        while (true) {

            String dashboardMenu = "1. Enter Credentials\n" +
                    "2. Forgot Password\n" +
                    "3. Go Back";

            engine.drawBox("LOGIN MENU", dashboardMenu, "\u001b[33m", false);

            String choice = scanner.nextLine();

            if (choice.equals("1"))
                return login();
            else if (choice.equals("2"))
                forgotPassword();
            else if (choice.equals("3") || choice.equalsIgnoreCase("back"))
                return null;
            else {
                System.out.println(engine.RED + engine.centerText("Invalid choice", engine.UI_WIDTH) + engine.RESET);
            }
        }
    }


    public User login() {

        int attempts = 0;

        while (attempts < 3) {

            System.out.println(engine.WHITE + engine.centerText("ENTER EMAIL: ", engine.UI_WIDTH) + engine.RESET);
            String email = scanner.nextLine().trim().toLowerCase();

            if (email.equalsIgnoreCase("back"))
                return null;

            System.out.println(engine.WHITE + engine.centerText("ENTER PASSWORD: ", engine.UI_WIDTH) + engine.RESET);
            String password = scanner.nextLine();

            if (password.equalsIgnoreCase("back"))
                return null;

            User user = findByEmail(email);

            String hashedInput = hashPassword(password);

if (user != null && user.getPassword().equals(hashedInput)) {

                System.out.println(engine.GREEN + engine.centerText("ACCESS GRANTED", engine.UI_WIDTH) + engine.RESET);

                if (user.isNewUser()) {
                    System.out.println(engine.YELLOW
                            + engine.centerText("Welcome " + user.getName() + "!", engine.UI_WIDTH) + engine.RESET);
                    user.setNewUser(false);
                    store.saveUsers(users, engine);
                }

                return user;
            }

            attempts++;
            System.out.println(engine.RED + engine.centerText(
                    "Invalid credentials (" + attempts + "/3)", engine.UI_WIDTH) + engine.RESET);
        }

        System.out.println(engine.RED + engine.centerText(
                "Too many attempts → Returning to menu", engine.UI_WIDTH) + engine.RESET);

        return null;
    }


    public void forgotPassword() {

        System.out.println(
                engine.WHITE + engine.centerText("ENTER REGISTERED MOBILE NUMBER: ", engine.UI_WIDTH) + engine.RESET);
        String mobile = scanner.nextLine().trim();

        if (mobile.equalsIgnoreCase("back"))
            return;

        User user = findByMobile(mobile);

        if (user == null) {
            System.out.println(engine.RED + engine.centerText("User not found", engine.UI_WIDTH) + engine.RESET);
            return;
        }

        System.out.println(engine.WHITE + engine.centerText("Account found for: " + user.getEmail(), engine.UI_WIDTH)
                + engine.RESET);

        System.out.println(engine.WHITE + engine.centerText("Reset Password? (Y/N)", engine.UI_WIDTH) + engine.RESET);
        String choice = scanner.nextLine();

        if (!choice.equalsIgnoreCase("Y"))
            return;

        while (true) {
            System.out
                    .println(engine.WHITE + engine.centerText("ENTER NEW PASSWORD: ", engine.UI_WIDTH) + engine.RESET);
            String newPass = scanner.nextLine();

            if (newPass.equalsIgnoreCase("back"))
                return;

            if (!Validator.isValidPassword(newPass, user.getName())) {
                System.out.println(engine.RED + engine.centerText(
                        "Minimum 8 characters (1 Upper letter, 1 Lower letter, 1 Special character, 1 Digit)",
                        engine.UI_WIDTH) + engine.RESET);
                continue;
            }

            System.out.println(
                    engine.WHITE + engine.centerText("CONFIRM NEW PASSWORD: ", engine.UI_WIDTH) + engine.RESET);
            String confirm = scanner.nextLine();

            if (!newPass.equals(confirm)) {
                System.out
                        .println(engine.RED + engine.centerText("Mismatch, try again", engine.UI_WIDTH) + engine.RESET);
                continue;
            }

            user.setPassword(hashPassword(newPass));
            store.saveUsers(users, engine);

            System.out.println(
                    engine.GREEN + engine.centerText("PASSWORD RESET SUCCESSFUL", engine.UI_WIDTH) + engine.RESET);
            break;
        }
    }


    private User findByEmail(String email) {
        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(email))
                return u;
        }
        return null;
    }

    private User findByMobile(String mobile) {
        for (User u : users) {
            if (u.getMobile().equals(mobile))
                return u;
        }
        return null;
    }
}

class Item {
    private final String name;
    private final String description;

    public Item(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}

class UsableItem extends Item {

    public UsableItem(String name, String description) {
        super(name, description);
    }

    public void use(Player player, GameEngine engine) {
        System.out.println(
                "\u001b[33m" +
                        engine.centerText("Item used.", engine.UI_WIDTH) +
                        "\u001b[0m");
    }
}

class HealthItem extends UsableItem {

    private final int healAmount;

    public HealthItem(String name, String description, int healAmount) {
        super(name, description);
        this.healAmount = healAmount;
    }

    @Override
    public void use(Player player, GameEngine engine) {
        player.heal(healAmount);

        System.out.println(
                "\u001b[32m" +
                        engine.centerText("+" + healAmount + " HEALTH RESTORED", engine.UI_WIDTH) +
                        "\u001b[0m");
    }

    public int getHealAmount() {
        return healAmount;
    }
}

class Inventory {

    private final List<Item> items;

    public Inventory() {
        this.items = new ArrayList<>();
    }


    public void addItem(Item item) {
        if (item != null) {
            items.add(item);
        }
    }

    public void removeItem(String name) {
        Item item = findItemByName(name);
        if (item != null) {
            items.remove(item);
        }
    }

    public boolean hasItem(String name) {
        return findItemByName(name) != null;
    }

    public List<Item> getItems() {
        return new ArrayList<>(items); 
    }


    public void useItem(String name, Player player, GameEngine engine) {

        Item item = findItemByName(name);

        if (item == null) {
            System.out.println(
                    engine.RED +
                            engine.centerText("Item not found.", engine.UI_WIDTH) +
                            engine.RESET);
            return;
        }

        if (!(item instanceof UsableItem)) {
            System.out.println(
                    engine.RED +
                            engine.centerText("Item cannot be used.", engine.UI_WIDTH) +
                            engine.RESET);
            return;
        }

        ((UsableItem) item).use(player, engine);

        items.remove(item);
    }


    public String getInventoryString() {
        if (items.isEmpty())
            return "Empty";

        StringBuilder inv = new StringBuilder();

        for (Item i : items) {
            inv.append("- ")
                    .append(i.getName())
                    .append(": ")
                    .append(i.getDescription())
                    .append("\n");
        }

        return inv.toString();
    }

    public void displayInventory(GameEngine engine) {

        if (items.isEmpty()) {
            System.out.println(
                    engine.WHITE +
                            engine.centerText("Inventory is empty.", engine.UI_WIDTH) +
                            engine.RESET);
            return;
        }

        System.out.println(
                engine.WHITE +
                        engine.centerText("Inventory:", engine.UI_WIDTH) +
                        engine.RESET);

        int index = 1;

        for (Item i : items) {
            System.out.println(
                    engine.YELLOW +
                            engine.centerText(
                                    "[" + index + "] " + i.getName() + ": " + i.getDescription(),
                                    engine.UI_WIDTH)
                            +
                            engine.RESET);
            index++; 
        }
    }


    private Item findItemByName(String name) {
        if (name == null)
            return null;

        for (Item i : items) {
            if (i.getName().equalsIgnoreCase(name.trim())) {
                return i;
            }
        }
        return null;
    }
}

class Player {

    private static final int MAX_HEALTH = 100;

    private final String name;
    private int health;
    private int score;
    private final Inventory inventory;
    private int currentRoomIndex;


    public Player(String name) {
        this(name, MAX_HEALTH, 0);
    }

    public Player(String name, int health, int score) {
        this.name = name;
        this.health = Math.min(Math.max(health, 0), MAX_HEALTH);
        this.score = Math.max(score, 0);
        this.inventory = new Inventory();
        this.currentRoomIndex = 0;
    }


    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public int getScore() {
        return score;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int getCurrentRoomIndex() {
        return currentRoomIndex;
    }


    public void setCurrentRoomIndex(int index) {
        if (index >= 0) {
            this.currentRoomIndex = index;
        }
    }


    public void addScore(int points) {
        if (points > 0) {
            this.score += points;
        }
    }

    public void reduceHealth(int amount) {
        if (amount <= 0)
            return;

        this.health -= amount;

        if (this.health < 0) {
            this.health = 0;
        }
    }

    public void heal(int amount) {
        if (amount <= 0)
            return;

        this.health += amount;

        if (this.health > MAX_HEALTH) {
            this.health = MAX_HEALTH;
        }
    }

    public boolean isAlive() {
        return this.health > 0;
    }


    public void useItem(String itemName, GameEngine engine) {
        inventory.useItem(itemName, this, engine);
    }

    public void collectItem(Item item) {
        inventory.addItem(item);
    }


    public void showStatus() {
        System.out.println("Player: " + name);
        System.out.println("Health: " + health);
        System.out.println("Score: " + score);
    }
}

class Puzzle {

    private static final int DEFAULT_DAMAGE = 20;

    private final String atmosphere;
    private final String question;
    private final List<String> answers;
    private final String[] hints;
    private final int points;

    private String requiredItem;
    private String useMessage;
    private Item rewardItem;

    protected String puzzleType;


    public Puzzle(String atmosphere, String question, List<String> answers, String h1, String h2, int points) {
        this.atmosphere = atmosphere;
        this.question = question;
        this.answers = normalizeAnswers(answers);
        this.hints = new String[] {
                (h1 != null ? h1 : ""),
                (h2 != null ? h2 : "No further hints available.")
        };
        this.points = Math.max(points, 0);

        this.requiredItem = null;
        this.useMessage = null;
        this.rewardItem = null;

        this.puzzleType = "RIDDLE";
    }

    public Puzzle(String atmosphere, String question, String answer, String h1, String h2, int points) {
        this(atmosphere, question, java.util.Arrays.asList(answer), h1, h2, points);
    }

    public Puzzle(String atmosphere, String question, List<String> answers, String h1, int points) {
        this(atmosphere, question, answers, h1, "No further hints available.", points);
    }


    public String getAtmosphere() {
        return atmosphere;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getAnswers() {
        return java.util.Collections.unmodifiableList(answers);
    }

    public String[] getHints() {
        return hints.clone();
    }

    public int getPoints() {
        return points;
    }

    public String getRequiredItem() {
        return requiredItem;
    }

    public String getUseMessage() {
        return useMessage;
    }

    public Item getRewardItem() {
        return rewardItem;
    }

    public String getPuzzleType() {
        return puzzleType;
    }


    public void setRequiredItem(String item, String message) {
        this.requiredItem = item;
        this.useMessage = message;
    }

    public void setRewardItem(Item item) {
        this.rewardItem = item;
    }

    public void setPuzzleType(String type) {
        if (type != null && !type.trim().isEmpty()) {
            this.puzzleType = type;
        }
    }


    public boolean validateAnswer(String input) {
        if (input == null)
            return false;

        String normalizedInput = normalize(input);

        for (String ans : answers) {
            if (ans.equals(normalizedInput)) {
                return true;
            }
        }
        return false;
    }

    public void onSolve(Player player) {
        player.addScore(points);

        if (rewardItem != null) {
            player.collectItem(rewardItem);
        }
    }

    public void onFail(Player player) {
        player.reduceHealth(DEFAULT_DAMAGE);
    }


    public void displayPuzzleInfo() {
        System.out.println("Type: " + puzzleType);
        System.out.println("Question: " + question);
    }


    private List<String> normalizeAnswers(List<String> rawAnswers) {
        List<String> list = new java.util.ArrayList<>();

        if (rawAnswers == null)
            return list;

        for (String ans : rawAnswers) {
            if (ans != null && !ans.trim().isEmpty()) {
                list.add(normalize(ans));
            }
        }

        return list;
    }

    private String normalize(String text) {
        return text.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}

abstract class BaseRoom {

    protected final String name;
    protected final String description;

    public BaseRoom(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public abstract List<Puzzle> getPuzzles();

    public abstract boolean isUnlocked();

    public abstract String getRequiredExitItem();

    public abstract String getUnlockMessage();

    public abstract void unlock();
}

class Room extends BaseRoom {

    private final List<Puzzle> puzzles;
    private String requiredExitItem;
    private String unlockMessage;
    private boolean isUnlocked;


    public Room(String name, String description) {
        super(name, description);
        this.puzzles = new ArrayList<>();
        this.requiredExitItem = null;
        this.unlockMessage = "THE EXIT REMAINS SEALED. A SPECIFIC KEY COMPONENT IS REQUIRED.";
        this.isUnlocked = true;
    }

    public Room(String name, String description, String requiredExitItem, String unlockMessage) {
        this(name, description);
        setExitRequirement(requiredExitItem, unlockMessage);
    }

    public Room(String name, String description, String requiredExitItem) {
        this(name, description, requiredExitItem,
                "THE EXIT IS SEALED. YOU NEED: " +
                        (requiredExitItem != null ? requiredExitItem.toUpperCase() : "UNKNOWN ITEM") + ".");
    }

    public List<Puzzle> getPuzzles() {
        return new ArrayList<>(puzzles); 
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public String getRequiredExitItem() {
        return requiredExitItem;
    }

    public String getUnlockMessage() {
        return unlockMessage;
    }

    public void unlock() {
        this.isUnlocked = true;
    }

    public void addPuzzle(Puzzle p) {
        if (p != null) {
            puzzles.add(p);
        }
    }

    public void setExitRequirement(String item, String message) {
        this.requiredExitItem = item;
        this.unlockMessage = (message != null && !message.trim().isEmpty())
                ? message
                : "THE EXIT IS SEALED. A REQUIRED ITEM IS MISSING.";
        this.isUnlocked = false;
    }
}

class GameEngine {
    private List<Room> rooms;
    private List<Puzzle> allPuzzles;
    private Player player;
    private Scanner scanner;
    private boolean isGameOver;
    final int UI_WIDTH = 150;

    public final String RESET = "\u001B[0m";
    public final String BOLD = "\u001B[1m";
    public final String RED = "\u001B[31m";
    public final String GREEN = "\u001B[32m";
    public final String YELLOW = "\u001B[33m";
    public final String BLUE = "\u001B[34m";
    public final String PURPLE = "\u001B[35m";
    public final String CYAN = "\u001B[36m";
    public final String WHITE = "\u001B[37m";
    public final String GRAY = "\u001B[90m";
    public final String BLINK = "\u001B[5m";

    public int getPlayerScore() {
        return player.getScore();
    }

    public GameEngine(Scanner scanner) {
        this.scanner = scanner;
        this.rooms = new ArrayList<>();
        this.allPuzzles = new ArrayList<>();
        this.isGameOver = false;
    }

    public String centerText(String text, int width) {
        if (text == null)
            return "";
        String s = text.replaceAll("\u001B\\[[;\\d]*m", "");
        int visualLength = s.length();
        if (visualLength >= width)
            return text;
        int totalPadding = width - visualLength;
        int leftPadding = totalPadding / 2;
        int rightPadding = totalPadding - leftPadding;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < leftPadding; i++)
            sb.append(" ");
        sb.append(text);
        for (int i = 0; i < rightPadding; i++)
            sb.append(" ");
        return sb.toString();
    }

    private void slowPrint(String text, int delay) {
        for (String line : text.split("\n")) {
            System.out.println(WHITE + centerText(line.trim(), UI_WIDTH) + RESET);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
            }
        }
    }

    private void showTransition() {
        try {
            for (int i = 0; i < 3; i++) {
                System.out.print("\r" + RED + BLINK + centerText(">> NEURAL SYNC RELOCATING <<", UI_WIDTH) + RESET);
                Thread.sleep(400);
                System.out.print("\r" + centerText("                              ", UI_WIDTH));
                Thread.sleep(200);
            }
            System.out.print("\r" + GREEN + centerText(">> ANOMALY ENTRANCE ESTABLISHED <<", UI_WIDTH) + RESET + "\n");
        } catch (Exception e) {
        }
    }

    private void displayDetailedStatus() {
        System.out.println("\n" + PURPLE + centerText(
                "================================================================================", UI_WIDTH) + RESET);
        System.out.println(
                PURPLE + centerText("║" + centerText("VITAL SIGN MONITORING ARRAY", 78) + "║", UI_WIDTH) + RESET);
        System.out.println(PURPLE + centerText(
                "--------------------------------------------------------------------------------", UI_WIDTH) + RESET);
        String healthCol = GREEN;
        if (player.getHealth() < 40)
            healthCol = RED;
        else if (player.getHealth() < 75)
            healthCol = YELLOW;

        System.out.println(
                PURPLE + centerText("║" + centerText("SUBJECT: " + player.getName().toUpperCase(), 78) + "║", UI_WIDTH)
                        + RESET);
        System.out.println(PURPLE + centerText(
                "║" + centerText("INTEGRITY: " + healthCol + player.getHealth() + "%" + RESET + PURPLE, 78) + "║",
                UI_WIDTH) + RESET);
        System.out.println(
                PURPLE + centerText("║" + centerText("CORE ENERGY: " + player.getScore(), 78) + "║", UI_WIDTH) + RESET);
        System.out.println(PURPLE + centerText(
                "║" + centerText("LOCATION SYNC: " + (player.getCurrentRoomIndex() + 1), 78) + "║", UI_WIDTH) + RESET);

        System.out.println(PURPLE + centerText(
                "║" + centerText("INVENTORY:", 78) + "║", UI_WIDTH) + RESET);

        String[] invLines = player.getInventory().getInventoryString().split("\n");

        for (String line : invLines) {
            System.out.println(PURPLE + centerText(
                    "║" + centerText(line.trim(), 78) + "║", UI_WIDTH) + RESET);
        }

        System.out.println(PURPLE + centerText(
                "================================================================================", UI_WIDTH) + RESET);
    }

    void displayTitle() {
        System.out.println();

        String[] title = {
                "██╗    ██╗███████╗██╗      ██████╗ ██████╗ ███╗   ███╗███████╗",
                "██║    ██║██╔════╝██║     ██╔════╝██╔═══██╗████╗ ████║██╔════╝",
                "██║ █╗ ██║█████╗  ██║     ██║     ██║   ██║██╔████╔██║█████╗  ",
                "██║███╗██║██╔══╝  ██║     ██║     ██║   ██║██║╚██╔╝██║██╔══╝  ",
                "╚███╔███╔╝███████╗███████╗╚██████╗╚██████╔╝██║ ╚═╝ ██║███████╗",
                " ╚══╝╚══╝ ╚══════╝╚══════╝ ╚═════╝ ╚═════╝ ╚═╝     ╚═╝╚══════╝"
        };

        String[] mid = {
                "████████╗ ██████╗ ",
                "╚══██╔══╝██╔═══██╗",
                "   ██║   ██║   ██║",
                "   ██║   ██║   ██║",
                "   ██║   ╚██████╔╝",
                "   ╚═╝    ╚═════╝ "
        };

        String[] bottom = {
                "███████╗███████╗ ██████╗ █████╗ ██████╗ ███████╗",
                "██╔════╝██╔════╝██╔════╝██╔══██╗██╔══██╗██╔════╝",
                "█████╗  ███████╗██║     ███████║██████╔╝█████╗  ",
                "██╔══╝  ╚════██║██║     ██╔══██║██╔═══╝ ██╔══╝  ",
                "███████╗███████║╚██████╗██║  ██║██║     ███████╗",
                "╚══════╝╚══════╝ ╚═════╝╚═╝  ╚═╝╚═╝     ╚══════╝"
        };

        String[] room = {
                "██████╗  ██████╗  ██████╗ ███╗   ███╗",
                "██╔══██╗██╔═══██╗██╔═══██╗████╗ ████║",
                "██████╔╝██║   ██║██║   ██║██╔████╔██║",
                "██╔══██╗██║   ██║██║   ██║██║╚██╔╝██║",
                "██║  ██║╚██████╔╝╚██████╔╝██║ ╚═╝ ██║",
                "╚═╝  ╚═╝ ╚═════╝  ╚═════╝ ╚═╝     ╚═╝"
        };

        System.out.println(
                CYAN + centerText("============================================================", UI_WIDTH) + RESET);
        System.out.println();

        for (String line : title) {
            System.out.println(CYAN + centerText(line, UI_WIDTH) + RESET);
        }

        System.out.println();

        for (String line : mid) {
            System.out.println(YELLOW + centerText(line, UI_WIDTH) + RESET);
        }

        System.out.println();

        for (String line : bottom) {
            System.out.println(GREEN + centerText(line, UI_WIDTH) + RESET);
        }

        System.out.println();

        for (String line : room) {
            System.out.println(PURPLE + centerText(line, UI_WIDTH) + RESET);
        }

        System.out.println();
        System.out.println(
                CYAN + centerText("============================================================", UI_WIDTH) + RESET);
        System.out.println();
    }

    private void displayVictory() {
        System.out.println();
        final String[] ASCII_VICTORY = {
                "██╗   ██╗██╗ ██████╗████████╗ ██████╗ ██████╗ ██╗   ██╗",
                "██║   ██║██║██╔════╝╚══██╔══╝██╔═══██╗██╔══██╗╚██╗ ██╔╝",
                "██║   ██║██║██║        ██║   ██║   ██║██████╔╝ ╚████╔╝ ",
                "╚██╗ ██╔╝██║██║        ██║   ██║   ██║██╔══██╗  ╚██╔╝  ",
                " ╚████╔╝ ██║╚██████╗   ██║   ╚██████╔╝██║  ██║   ██║   ",
                "  ╚═══╝  ╚═╝ ╚═════╝   ╚═╝    ╚═════╝ ╚═╝  ╚═╝   ╚═╝   "
        };
        for (String line : ASCII_VICTORY) {
            System.out.println(GREEN + centerText(line, UI_WIDTH) + RESET);
        }
        System.out.println();
    }

    private void displayGameOver() {
        final String[] ASCII_GAME_OVER = {
                " ██████╗  █████╗ ███╗   ███╗███████╗ ",
                "██╔════╝ ██╔══██╗████╗ ████║██╔════╝ ",
                "██║  ███╗███████║██╔████╔██║█████╗   ",
                "██║   ██║██╔══██║██║╚██╔╝██║██╔══╝   ",
                "╚██████╔╝██║  ██║██║ ╚═╝ ██║███████╗ ",
                " ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚═╝╚══════╝ ",
                "",
                " ██████╗ ██╗   ██╗███████╗██████╗ ",
                "██╔═══██╗██║   ██║██╔════╝██╔══██╗",
                "██║   ██║██║   ██║█████╗  ██████╔╝",
                "██║   ██║██║   ██║██╔══╝  ██╔══██╗",
                "╚██████╔╝╚██████╔╝███████╗██║  ██║",
                " ╚═════╝  ╚═════╝ ╚══════╝╚═╝  ╚═╝"
        };
        for (String line : ASCII_GAME_OVER) {
            System.out.println(RED + centerText(line, UI_WIDTH) + RESET);
        }
    }

    private void displayLevelHeader(int level, String name) {
        System.out.println("\n\n");

        System.out.println(BLUE + centerText(
                "╔══════════════════════════════════════════════════════════════════════════════╗", UI_WIDTH) + RESET);
        System.out.println(
                BLUE + centerText("║" + centerText("LEVEL " + level + " - " + name, 78) + "║", UI_WIDTH) + RESET);
        System.out.println(BLUE + centerText(
                "╚══════════════════════════════════════════════════════════════════════════════╝", UI_WIDTH) + RESET);

        System.out.println();

        String[] levelText = {
                "██╗     ███████╗██╗   ██╗███████╗██╗     ",
                "██║     ██╔════╝██║   ██║██╔════╝██║     ",
                "██║     █████╗  ██║   ██║█████╗  ██║     ",
                "██║     ██╔══╝  ╚██╗ ██╔╝██╔══╝  ██║     ",
                "███████╗███████╗ ╚████╔╝ ███████╗███████╗",
                "╚══════╝╚══════╝  ╚═══╝  ╚══════╝╚══════╝"
        };

        for (String line : levelText) {
            System.out.println(BLUE + centerText(line, UI_WIDTH) + RESET);
        }

        System.out.println();

        String[] number;

        if (level == 1) {
            number = new String[] {
                    "  ██╗  ",
                    " ███║  ",
                    " ╚██║  ",
                    "  ██║  ",
                    "  ██║  ",
                    "  ╚═╝  "
            };
        } else if (level == 2) {
            number = new String[] {
                    " ██████╗ ",
                    " ╚════██╗",
                    "  █████╔╝",
                    " ██╔═══╝ ",
                    " ███████╗",
                    " ╚══════╝"
            };
        } else {
            number = new String[] {
                    " ██████╗ ",
                    " ╚════██╗",
                    "  █████╔╝",
                    "  ╚═══██╗",
                    " ██████╔╝",
                    " ╚═════╝ "
            };
        }

        for (String line : number) {
            System.out.println(YELLOW + centerText(line, UI_WIDTH) + RESET);
        }

        System.out.println("\n");
    }

    void drawBox(String title, String content, String color, boolean center) {
        int boxWidth = 80;
        int contentWidth = boxWidth - 4;

        String horizontal = "═".repeat(boxWidth - 2);

        String cleanTitle = title.replaceAll("\u001B\\[[;\\d]*m", "");

        System.out.println(color + centerText("╔" + horizontal + "╗", UI_WIDTH) + RESET);

        String titleLine = title;
        if (center) {
            int padding = (contentWidth - cleanTitle.length()) / 2;
            if (padding < 0)
                padding = 0;
            titleLine = " ".repeat(padding) + title;
        }
        titleLine = padRight(titleLine, contentWidth);

        System.out.println(color + centerText("║ " + titleLine + " ║", UI_WIDTH) + RESET);

        System.out.println(color + centerText("╠" + horizontal + "╣", UI_WIDTH) + RESET);

        content = content.replace("||", "").trim();

        String[] lines = content.split("\n");

        for (String rawLine : lines) {
            String lineContent = rawLine.trim();

            while (lineContent.length() > 0) {
                int end = Math.min(contentWidth, lineContent.length());

                if (end < lineContent.length() && lineContent.charAt(end) != ' ') {
                    int lastSpace = lineContent.lastIndexOf(' ', end);
                    if (lastSpace > 0)
                        end = lastSpace;
                }

                String line = lineContent.substring(0, end).trim();
                lineContent = lineContent.substring(end).trim();

                String cleanLine = line.replaceAll("\u001B\\[[;\\d]*m", "");

                if (center) {
                    int padding = (contentWidth - cleanLine.length()) / 2;
                    if (padding < 0)
                        padding = 0;
                    line = " ".repeat(padding) + line;
                }

                line = padRight(line, contentWidth);

                System.out.println(color + centerText("║ " + line + " ║", UI_WIDTH) + RESET);
            }
        }

        System.out.println(color + centerText("╚" + horizontal + "╝", UI_WIDTH) + RESET);
    }

    private String padRight(String text, int width) {
        if (text.length() >= width)
            return text;
        return text + " ".repeat(width - text.length());
    }

    private void showProcessing() {
        try {
            System.out.println(GRAY + centerText("PROCESSING SYSTEM REQUEST", UI_WIDTH) + RESET);

            for (int i = 0; i < 3; i++) {
                Thread.sleep(300);
                System.out.print(GRAY + "." + RESET);
            }

            Thread.sleep(200);
            System.out.println();
        } catch (InterruptedException e) {
        }
    }

    private void displayActionMenu() {
        String menu = "[1] Answer Puzzle\n" +
                "[2] Use Item (from inventory)\n" +
                "[3] View Inventory ('inventory')\n" +
                "[4] Get Hint ('hint', -10 HP)\n" +
                "[5] Show Status ('status')";
        drawBox("AVAILABLE ACTIONS", menu, WHITE, false);
    }

    public void startGame(User user) {
    this.isGameOver = false;

    player = new Player(user.getName(), 100, 0); 

    System.out.println("\n" + YELLOW +
            centerText("WELCOME, " + player.getName().toUpperCase() + ". THE DOORS SEAL BEHIND YOU.", UI_WIDTH)
            + RESET);

    System.out.println(GRAY + centerText(
            "PREVIOUS HIGH SCORE: " + user.getHighScore(),
            UI_WIDTH
    ) + RESET);

    System.out.println(YELLOW + centerText("CHOOSE YOUR TEST DEPTH:", UI_WIDTH) + RESET);
    System.out.println(WHITE + centerText("1. SHALLOW (EASY - 1 Puzzle per Room)", UI_WIDTH - 7) + RESET);
    System.out.println(WHITE + centerText("2. MERSIVE (MEDIUM - 2 Puzzles per Room)", UI_WIDTH - 3) + RESET);
    System.out.println(WHITE + centerText("3. PROFOUND (HARD - 3 Puzzles per Room)", UI_WIDTH - 5) + RESET);
    System.out.println(WHITE + centerText("SELECTION: ", UI_WIDTH));

    String choice = "";
    if (scanner.hasNextLine()) {
        choice = scanner.nextLine();
    }

    initializeGame(choice);
    runGameLoop();
}

    private void initializeGame(String choice) {
        allPuzzles.clear();
        rooms.clear();
        addPuzzlesToPool();
        Collections.shuffle(allPuzzles);
        if (!choice.equals("1") && !choice.equals("2") && !choice.equals("3")) {
            System.out.println(RED + centerText("Invalid Choice", UI_WIDTH));
            System.out.println(WHITE + centerText("Enter a valid choice (1, 2, or 3):", UI_WIDTH));
            choice = scanner.nextLine();
            while (!choice.equals("1") && !choice.equals("2") && !choice.equals("3")) {
                System.out.println(RED + centerText("Invalid Choice", UI_WIDTH));
                System.out.println(WHITE + centerText("Enter a valid choice (1, 2, or 3):", UI_WIDTH));
                choice = scanner.nextLine();
            }
            System.out.println(GREEN + centerText(
                    "DIFFICULTY SET TO "
                            + (choice.equals("1") ? "SHALLOW" : choice.equals("2") ? "MERSIVE" : "PROFOUND") + ".",
                    UI_WIDTH) + RESET);
        } else {
            System.out.println(GREEN + centerText(
                    "DIFFICULTY SET TO "
                            + (choice.equals("1") ? "SHALLOW" : choice.equals("2") ? "MERSIVE" : "PROFOUND") + ".",
                    UI_WIDTH) + RESET);
        }
        int puzzlesPerRoom = 1;
        if (choice.equals("2"))
            puzzlesPerRoom = 2;
        else if (choice.equals("3"))
            puzzlesPerRoom = 3;

        Room atrium = new Room("THE OBSIDIAN ATRIUM",
                "THE OBSIDIAN ATRIUM: A hall of dark polished stone.\nShadows move with a life of their own.\nThe air is thick with the weight of unseen eyes.");
        Room crypt = new Room("THE EMERALD CRYPT",
                "THE EMERALD CRYPT: Green phosphorescent moss illuminates the wet stone.\nVials of chemicals line the rotted wooden shelves.\nThe sound of dripping echoes in the deep shadows.");
        Room lab = new Room("THE COBALT LABORATORIUM",
                "THE COBALT LABORATORIUM: Blue light and chrome machinery hum with power.\nBrass tubes snake across the high ceiling.\nThe air is frigid and bites at your skin.");

        atrium.setExitRequirement("Obsidian Keycard",
                "A BIOMETRIC SCANNER BLINKS RED. IT REQUIRES AN OBSIDIAN CORE COMPONENT.");
        crypt.setExitRequirement("Emerald Data-Link", "THE MAG-LOCK REQUIRES A PHOSPHORESCENT SIGNAL SYNC.");

        rooms.add(atrium);
        rooms.add(crypt);
        rooms.add(lab);

        player.collectItem(
                new HealthItem("Medkit", "A compact emergency medical kit for rapid integrity restoration.", 50));
        int pIndex = 0;
        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            for (int j = 0; j < puzzlesPerRoom; j++) {
                if (pIndex < allPuzzles.size()) {
                    Puzzle p = allPuzzles.get(pIndex++);
                    r.addPuzzle(p);

                    if (j == puzzlesPerRoom - 1) {
                        if (i == 0)
                            p.setRewardItem(new Item("Obsidian Keycard", "A dark, heavy card that emits a faint hum."));
                        else if (i == 1)
                            p.setRewardItem(new Item("Emerald Data-Link",
                                    "A glowing green module etched with crystalline circuits."));
                    }
                }
            }
        }
    }

    private void runGameLoop() {
        while (!isGameOver) {

            Room current = rooms.get(player.getCurrentRoomIndex());

            showTransition();
            displayLevelHeader(player.getCurrentRoomIndex() + 1, current.getName());
            displayDetailedStatus();

            slowPrint(current.getDescription(), 100);

            for (Puzzle p : current.getPuzzles()) {
                if (!solvePuzzle(p)) {
                    isGameOver = true;
                    break;
                }
            }

            if (isGameOver) {
                displayGameOver();
                break;
            }

            if (!handleRoomExit(current)) {
                isGameOver = true;
                break;
            }

            player.setCurrentRoomIndex(player.getCurrentRoomIndex() + 1);

            if (player.getCurrentRoomIndex() >= rooms.size()) {
                displayVictory();
                isGameOver = true;
            }
        }
    }

    private boolean handleRoomExit(Room r) {
        if (r.isUnlocked())
            return true;

        drawBox("ACCESS DENIED", "NEXT CHAMBER SEALED\n" + r.getUnlockMessage(), YELLOW, true);

        while (!r.isUnlocked()) {
            displayActionMenu();
            System.out.println(CYAN + centerText("INTEGRITY HUB INPUT: ", UI_WIDTH) + RESET);

            if (!scanner.hasNextLine())
                return false;

            String input = scanner.nextLine().trim().toLowerCase();
            showProcessing();

            if (input.equals("3") || input.equals("inventory") || input.equals("items")) {
                drawBox("INVENTORY",
                        player.getInventory().getInventoryString(),
                        CYAN,
                        false);
                continue;
            }

            if (input.equals("4") || input.equals("hint")) {
                drawBox("HINT",
                        "ANALYSIS: SEARCH YOUR CARGO FOR COMPONENTS RECOVERED IN THIS CHAMBER.",
                        YELLOW,
                        true);
                continue;
            }

            String itemName = resolveItemInput(input);

            if (!itemName.isEmpty()) {

                if (!player.getInventory().hasItem(itemName)) {
                    drawBox("ERROR",
                            "ITEM NOT FOUND IN INVENTORY",
                            RED,
                            true);
                    continue;
                }

                if (r.getRequiredExitItem() != null &&
                        itemName.equalsIgnoreCase(r.getRequiredExitItem())) {

                    player.getInventory().removeItem(itemName);
                    r.unlock();

                    drawBox("SUCCESS",
                            "USING " + itemName.toUpperCase() + " TO UNLOCK NEXT DOOR...\nACCESS GRANTED.",
                            GREEN,
                            true);

                    return true;
                } else {

                    player.useItem(itemName, this);

                    drawBox("SYSTEM LOG",
                            "[ " + itemName.toUpperCase() + " ] HAS NO EFFECT HERE.",
                            YELLOW,
                            true);
                }

                continue;
            }

            if (input.equals("1")) {
                drawBox("SYSTEM LOG",
                        "PUZZLE IS ALREADY SOLVED.\nACCESS THE EXIT HUB BY USING THE CORRECT KEY COMPONENT.",
                        YELLOW,
                        true);
            } else {
                drawBox("INVALID COMMAND", "THE SEQUENCE HUB IS WAITING.", RED, true);
            }
        }

        return true;
    }

    private boolean solvePuzzle(Puzzle p) {

        drawBox("ENVIRONMENT LOG", p.getAtmosphere(), GRAY, false);

        System.out.println();
        for (String line : p.getQuestion().split("\n")) {
            System.out.println(WHITE + BOLD + centerText("QUESTION: " + line.trim(), UI_WIDTH) + RESET);
        }

        int hintCount = 0;

        while (true) {

            displayActionMenu();
            System.out.println(CYAN + centerText("RESPONSE INPUT:", UI_WIDTH) + RESET);

            if (!scanner.hasNextLine())
                return false;

            String input = scanner.nextLine().trim().toLowerCase();
            showProcessing();

            if (input.equals("5") || input.equals("status")) {
                displayDetailedStatus();
                continue;
            }

            if (input.equals("4") || input.equals("hint")) {
                if (hintCount < p.getHints().length) {
                    drawBox("HINT", p.getHints()[hintCount] + "\n-10 HP", YELLOW, true);
                    hintCount++;
                    player.reduceHealth(10);
                } else {
                    drawBox("NO DATA", "NO FURTHER DATA AVAILABLE.", RED, true);
                }
                continue;
            }

            if (input.equals("3") || input.equals("inventory") || input.equals("items")) {
                drawBox("INVENTORY",
                        player.getInventory().getInventoryString(),
                        CYAN,
                        false);
                continue;
            }

            String itemName = "";

            if (input.equals("2")) {

                drawBox("INVENTORY",
                        player.getInventory().getInventoryString(),
                        CYAN,
                        false);

                if (player.getInventory().getItems().isEmpty())
                    continue;

                System.out.print(CYAN + centerText("SELECT ITEM NUMBER OR NAME (or 'back'):", UI_WIDTH) + RESET);
                System.out.println(centerText("> ", UI_WIDTH));

                String itemChoice = scanner.nextLine().trim().toLowerCase();

                if (itemChoice.equals("back"))
                    continue;

                try {
                    int idx = Integer.parseInt(itemChoice) - 1;
                    if (idx >= 0 && idx < player.getInventory().getItems().size()) {
                        itemName = player.getInventory().getItems().get(idx).getName();
                    } else {
                        itemName = itemChoice;
                    }
                } catch (NumberFormatException e) {
                    itemName = itemChoice;
                }

            } else if (input.startsWith("use ")) {
                itemName = input.substring(4).trim();
            }

            if (!itemName.isEmpty()) {

                if (!player.getInventory().hasItem(itemName)) {
                    drawBox("ERROR",
                            "ITEM NOT FOUND IN INVENTORY",
                            RED,
                            true);
                    continue;
                }

                if (p.getRequiredItem() != null && itemName.equalsIgnoreCase(p.getRequiredItem())) {

                    player.useItem(itemName, this);

                    drawBox("ITEM ACTIVATED",
                            p.getUseMessage() + "\nANALYSIS BOOSTED.\nDATA: " + p.getHints()[0],
                            GREEN, true);

                } else {

                    boolean isUsable = false;
                    int healAmount = 0;

                    for (Item i : player.getInventory().getItems()) {
                        if (i.getName().equalsIgnoreCase(itemName)) {

                            if (i instanceof HealthItem) {
                                isUsable = true;
                                healAmount = ((HealthItem) i).getHealAmount();
                            } else if (i instanceof UsableItem) {
                                isUsable = true;
                            }

                            break;
                        }
                    }

                    player.useItem(itemName, this);

                    if (healAmount > 0) {
                        drawBox("SYSTEM",
                                ">> INTEGRITY RESTORED BY " + healAmount + "% <<",
                                GREEN,
                                true);
                    }

                    else if (!isUsable) {
                        drawBox("SYSTEM LOG",
                                "[ " + itemName.toUpperCase() + " ] HAS NO EFFECT HERE.",
                                YELLOW, true);
                    }
                }

                continue;
            }

            String answerAttempt = input;

            if (input.equals("1")) {

                System.out.println();
                for (String line : p.getQuestion().split("\n")) {
                    System.out.println(WHITE + BOLD + centerText("QUESTION: " + line.trim(), UI_WIDTH) + RESET);
                }

                System.out.println(CYAN + centerText("SUBMIT YOUR ANSWER:", UI_WIDTH) + RESET);

                if (!scanner.hasNextLine())
                    return false;

                answerAttempt = scanner.nextLine().trim().toLowerCase();
                showProcessing();
            }

            if (p.validateAnswer(answerAttempt)) {

                p.onSolve(player);

                String rewardMsg = "";
                if (p.getRewardItem() != null) {
                    rewardMsg = "ITEM ACQUIRED: " + p.getRewardItem().getName().toUpperCase();
                }

                drawBox("PUZZLE SOLVED",
                        "ACCESS GRANTED.\n" + rewardMsg,
                        GREEN,
                        true);

                displayDetailedStatus();

                return true;

            } else if (!input.equals("1")) {

                if (input.equals("2") || input.equals("3") || input.equals("4") || input.equals("5"))
                    continue;

                drawBox("INVALID COMMAND", "ENTER A VALID RESPONSE.", RED, true);
                continue;

            } else {

                p.onFail(player);

                drawBox("WRONG ANSWER", "-20 HP", RED, true);

                displayDetailedStatus();

                if (player.getHealth() <= 0) {
                    drawBox("GAME OVER", "PLAYER TERMINATED.", RED, true);
                    return false;
                }
            }
        }
    }

    private String resolveItemInput(String input) {

        if (input.equals("2")) {

            drawBox("INVENTORY",
                    player.getInventory().getInventoryString(),
                    CYAN,
                    false);

            if (player.getInventory().getItems().isEmpty())
                return "";

            System.out.print(CYAN + centerText("SELECT ITEM NUMBER OR NAME (or 'back'):", UI_WIDTH) + RESET);
            System.out.println(centerText("> ", UI_WIDTH));

            String itemChoice = scanner.nextLine().trim().toLowerCase();

            if (itemChoice.equals("back"))
                return "";

            try {
                int idx = Integer.parseInt(itemChoice) - 1;
                List<Item> items = player.getInventory().getItems();

                if (idx >= 0 && idx < items.size()) {
                    return items.get(idx).getName();
                }
            } catch (NumberFormatException e) {
            }

            return itemChoice;

        } else if (input.startsWith("use ")) {
            return input.substring(4).trim();
        }

        return "";
    }

    private void addPuzzlesToPool() {

        allPuzzles.add(new Puzzle(
                "An ancient grand instrument stands under a dusty spotlight, untouched for years.",
                "I have 88 keys but cannot open a single lock. What am I?",
                Arrays.asList("piano", "a piano"),
                "Found in music halls.",
                "Black and white keys.",
                50));

        allPuzzles.add(new Puzzle(
                "The lights flicker and suddenly everything fades into obscurity.",
                "The more of me you have, the less you can see. What am I?",
                Arrays.asList("darkness", "shadow"),
                "Opposite of light.",
                "Appears when lights go off.",
                50));

        allPuzzles.add(new Puzzle(
                "A fragile object sits delicately on a silver plate.",
                "What must be broken before it can be used?",
                Arrays.asList("egg", "an egg"),
                "Breakfast item.",
                "Has shell and yolk.",
                50));

        allPuzzles.add(new Puzzle(
                "A dim candle flickers, slowly melting away.",
                "I grow shorter the longer I live. What am I?",
                Arrays.asList("candle"),
                "Provides light.",
                "Melts while burning.",
                50));

        allPuzzles.add(new Puzzle(
                "A dusty calendar hangs crookedly on the wall.",
                "How many months have at least 28 days?",
                Arrays.asList("12", "all of them", "all"),
                "Think carefully.",
                "Every month has 28 days.",
                50));

        allPuzzles.add(new Puzzle(
                "A soaked cleaning tool lies beside a bucket.",
                "I am full of holes, yet I can hold water. What am I?",
                Arrays.asList("sponge"),
                "Used for cleaning.",
                "Absorbs water.",
                50));

        allPuzzles.add(new Puzzle(
                "A mysterious voice echoes in the room.",
                "What question can you never answer 'yes' to truthfully?",
                Arrays.asList("are you asleep"),
                "Logical trick.",
                "Think about awareness.",
                50));

        allPuzzles.add(new Puzzle(
                "A glowing orb shows visions of time yet to come.",
                "I am always ahead of you but can never be seen. What am I?",
                Arrays.asList("future"),
                "Time related.",
                "Not happened yet.",
                50));

        allPuzzles.add(new Puzzle(
                "A bright yellow house blueprint appears.",
                "In a one-story house, what color are the stairs?",
                Arrays.asList("none"),
                "No second floor.",
                "No stairs exist.",
                50));

        allPuzzles.add(new Puzzle(
                "A whisper bounces endlessly across the walls.",
                "I speak without a mouth and hear without ears. What am I?",
                Arrays.asList("echo", "an echo"),
                "Sound reflection.",
                "Repeats what you say.",
                60));

        allPuzzles.add(new Puzzle(
                "A massive map is spread across a wooden table.",
                "I have cities, rivers, and roads but no people. What am I?",
                Arrays.asList("map"),
                "Used for navigation.",
                "Flat representation.",
                60));

        allPuzzles.add(new Puzzle(
                "A glowing device sits before you.",
                "I have keys but no locks, space but no room. What am I?",
                Arrays.asList("keyboard"),
                "Used with computers.",
                "You type on it.",
                60));

        allPuzzles.add(new Puzzle(
                "A damp cloth hangs nearby.",
                "What gets wetter the more it dries?",
                Arrays.asList("towel"),
                "Used after bath.",
                "Absorbs water.",
                60));

        allPuzzles.add(new Puzzle(
                "A mirror shows your reflection but whispers something deeper.",
                "What belongs to you but is used more by others?",
                Arrays.asList("name", "our name"),
                "Your identity.",
                "People call you by it.",
                60));

        allPuzzles.add(new Puzzle(
                "A strange word glows in mid-air.",
                "What word becomes shorter when you add two letters?",
                Arrays.asList("short"),
                "Think grammar.",
                "Add 'er'.",
                70));

        allPuzzles.add(new Puzzle(
                "A logical family puzzle appears.",
                "A girl has as many brothers as sisters. Each boy has twice as many sisters as brothers. How many children are there?",
                Arrays.asList("7", "seven", "4 sisters and 3 brothers", "4 girls and 3 boys"),
                "Break it logically.",
                "4 girls, 3 boys.",
                70));

        allPuzzles.add(new Puzzle(
                "A glowing letter appears in darkness.",
                "What is the beginning of everything and the end of everywhere?",
                Arrays.asList("e"),
                "Look at spelling.",
                "Common letter.",
                70));

        allPuzzles.add(new Puzzle(
                "An envelope lies sealed on the table.",
                "What travels around the world while staying in one corner?",
                Arrays.asList("stamp"),
                "Used in mail.",
                "Sticks to envelope.",
                70));

        allPuzzles.add(new Puzzle(
                "A heavy wooden box rests silently.",
                "The maker doesn't need it, the buyer doesn't use it. What is it?",
                Arrays.asList("coffin"),
                "Related to death.",
                "Used after life.",
                70));

        allPuzzles.add(new Puzzle(
                "A coin spins endlessly in the air.",
                "I have a head and a tail but no body. What am I?",
                Arrays.asList("coin", "a coin"),
                "Used as money.",
                "Flip it.",
                70));

        allPuzzles.add(new Puzzle(
                "A ticking device echoes in the silence.",
                "I have a face and hands but no arms or legs. What am I?",
                Arrays.asList("clock"),
                "Shows time.",
                "Mounted on wall.",
                70));

        allPuzzles.add(new Puzzle(
                "The room falls completely silent.",
                "What is so fragile that saying its name breaks it?",
                Arrays.asList("silence"),
                "Opposite of noise.",
                "Breaks when spoken.",
                70));

        allPuzzles.add(new Puzzle(
                "Dusty footprints appear on the floor.",
                "The more you take, the more you leave behind. What are they?",
                Arrays.asList("footsteps", "footprints", "steps", "foot steps"),
                "Walking related.",
                "Marks on ground.",
                70));

        allPuzzles.add(new Puzzle(
                "A blazing flame dances before you.",
                "I live when fed and die when given water. What am I?",
                Arrays.asList("fire"),
                "Needs oxygen.",
                "Extinguished by water.",
                50));

        allPuzzles.add(new Puzzle(
                "A locked chest hums with energy.",
                "I can fly without wings, cry without eyes. Wherever I go, darkness flies. What am I?",
                Arrays.asList("cloud", "a cloud", "clouds"),
                "Seen in sky.",
                "Brings rain.",
                60));

        allPuzzles.add(new Puzzle(
                "A glowing riddle appears on the wall.",
                "The more you remove from me, the bigger I get. What am I?",
                Arrays.asList("hole"),
                "Think opposite logic.",
                "Digging creates it.",
                70));

        allPuzzles.add(new Puzzle(
                "A strange shadow stretches across the floor.",
                "I follow you all the time but disappear in darkness. What am I?",
                Arrays.asList("shadow"),
                "Needs light.",
                "Your outline.",
                50));

        allPuzzles.add(new Puzzle(
                "A mysterious lock requires a clever answer.",
                "What has one eye but cannot see?",
                Arrays.asList("needle"),
                "Used in stitching.",
                "Has thread hole.",
                60));

        allPuzzles.add(new Puzzle(
                "A magical scroll unrolls itself.",
                "I shave every day, but my beard stays the same. Who am I?",
                Arrays.asList("barber"),
                "Profession.",
                "Cuts hair.",
                70));

        allPuzzles.add(new Puzzle(
                "A strange machine asks for input.",
                "What has many teeth but cannot bite?",
                Arrays.asList("comb"),
                "Used for hair.",
                "Straightens hair.",
                50));

        allPuzzles.add(new Puzzle(
                "A heavy gate blocks your path.",
                "What begins with T, ends with T, and has T in it?",
                Arrays.asList("teapot"),
                "Kitchen item.",
                "Holds tea.",
                60));

        allPuzzles.add(new Puzzle(
                "A glowing symbol spins slowly.",
                "I am not alive, but I grow. I don’t have lungs, but I need air. What am I?",
                Arrays.asList("fire"),
                "Needs oxygen.",
                "Dangerous.",
                60));

        allPuzzles.add(new Puzzle(
                "A cold breeze whispers a puzzle.",
                "What goes up but never comes down?",
                Arrays.asList("age"),
                "Time related.",
                "Increases always.",
                50));

        allPuzzles.add(new Puzzle(
                "A locked door has a numeric keypad.",
                "If two's company and three's a crowd, what are four and five?",
                Arrays.asList("nine", "9", "four and five", "4 and 5"),
                "Simple math.",
                "Add them.",
                50));

        allPuzzles.add(new Puzzle(
                "A mysterious voice echoes again.",
                "What has hands but cannot clap?",
                Arrays.asList("clock"),
                "Shows time.",
                "Has hour and minute hands.",
                50));

        allPuzzles.add(new Puzzle(
                "A glowing chest reveals another riddle.",
                "What can fill a room but takes up no space?",
                Arrays.asList("light", "a light", "air", "gas"),
                "Opposite of darkness.",
                "Invisible but present.",
                60));

        allPuzzles.add(new Puzzle(
                "A strange note lies on the floor.",
                "If you drop me, I'm sure to crack. But give me a smile, and I'll smile back. What am I?",
                Arrays.asList("mirror"),
                "Reflects you.",
                "Fragile.",
                60));

        allPuzzles.add(new Puzzle(
                "A golden key floats mid-air.",
                "What has a neck but no head, two arms but no hands?",
                Arrays.asList("shirt"),
                "Clothing.",
                "Worn on body.",
                70));

        allPuzzles.add(new Puzzle(
                "A puzzle box clicks open slightly.",
                "What is always coming but never arrives?",
                Arrays.asList("tomorrow"),
                "Time concept.",
                "Future day.",
                60));

        allPuzzles.add(new Puzzle(
                "A strange object vibrates softly.",
                "I have branches, but no fruit, trunk, or leaves. What am I?",
                Arrays.asList("bank"),
                "Money related.",
                "Stores wealth.",
                70));

        allPuzzles.add(new Puzzle(
                "A dusty board shows numbers.",
                "What 3 numbers give the same result when multiplied and added together?",
                Arrays.asList("1,2,3", "1 2 3", "123"),
                "Simple math.",
                "1 x 2 x 3 = 6 and 1 + 2 + 3 = 6.",
                80));

        allPuzzles.add(new Puzzle(
                "A mysterious lock glows faintly.",
                "I am tall when I am young, and short when I am old. What am I?",
                Arrays.asList("candle"),
                "Melts over time.",
                "Gives light.",
                50));

        allPuzzles.add(new Puzzle(
                "A hidden drawer slides open.",
                "What gets bigger the more you take away from it?",
                Arrays.asList("hole"),
                "Think removal.",
                "Digging.",
                70));
    }
}
