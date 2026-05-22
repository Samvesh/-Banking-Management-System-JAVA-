import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class BankingManagementSystem {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final Bank BANK = new Bank(Path.of("bank-data-java.txt"));

    public static void main(String[] args) {
        BANK.load();
        while (true) {
            printMenu();
            int choice = readInt("Choose an option: ");
            try {
                switch (choice) {
                    case 1 -> createAccount();
                    case 2 -> deposit();
                    case 3 -> withdraw();
                    case 4 -> transfer();
                    case 5 -> applyInterest();
                    case 6 -> openFixedDeposit();
                    case 7 -> createLoan();
                    case 8 -> payEmi();
                    case 9 -> showAccount();
                    case 10 -> showHistory();
                    case 11 -> BANK.listAccounts();
                    case 12 -> {
                        BANK.save();
                        System.out.println("Data saved. Goodbye.");
                        return;
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (BankException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
            BANK.save();
        }
    }

    private static void printMenu() {
        System.out.println("\n==== Banking Management System ====");
        System.out.println("1. Create account");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Transfer");
        System.out.println("5. Apply yearly interest / current fee");
        System.out.println("6. Open fixed deposit");
        System.out.println("7. Create EMI loan");
        System.out.println("8. Pay EMI");
        System.out.println("9. Account details");
        System.out.println("10. Transaction history");
        System.out.println("11. List all accounts");
        System.out.println("12. Save and exit");
    }

    private static void createAccount() {
        String holder = readLine("Account holder name: ");
        String type = readLine("Type (saving/current): ").trim().toLowerCase();
        BigDecimal initial = readMoney("Initial deposit: ");
        Account account;
        if (type.equals("saving")) {
            BigDecimal rate = readMoney("Annual interest rate (%): ");
            account = Account.savings(BANK.nextAccountNumber(), holder, initial, rate);
        } else if (type.equals("current")) {
            BigDecimal fee = readMoney("Monthly maintenance fee: ");
            account = Account.current(BANK.nextAccountNumber(), holder, initial, fee);
        } else {
            throw new BankException("Account type must be saving or current.");
        }
        BANK.addAccount(account);
        System.out.println("Created account number: " + account.accountNumber);
    }

    private static void deposit() {
        Account account = BANK.find(readLong("Account number: "));
        BigDecimal amount = readMoney("Deposit amount: ");
        account.deposit(amount, "Cash deposit");
        System.out.println("Deposited. Balance: " + money(account.balance));
    }

    private static void withdraw() {
        Account account = BANK.find(readLong("Account number: "));
        BigDecimal amount = readMoney("Withdraw amount: ");
        account.withdraw(amount, "Cash withdrawal");
        System.out.println("Withdrawn. Balance: " + money(account.balance));
    }

    private static void transfer() {
        Account from = BANK.find(readLong("From account number: "));
        Account to = BANK.find(readLong("To account number: "));
        BigDecimal amount = readMoney("Transfer amount: ");
        from.withdraw(amount, "Transfer to " + to.accountNumber);
        to.deposit(amount, "Transfer from " + from.accountNumber);
        System.out.println("Transfer complete.");
    }

    private static void applyInterest() {
        Account account = BANK.find(readLong("Account number: "));
        account.applyAccountRule();
        System.out.println("Updated balance: " + money(account.balance));
    }

    private static void openFixedDeposit() {
        Account account = BANK.find(readLong("Account number: "));
        BigDecimal amount = readMoney("FD amount: ");
        BigDecimal annualRate = readMoney("FD annual rate (%): ");
        int months = readInt("FD duration in months: ");
        account.openFixedDeposit(amount, annualRate, months);
        System.out.println("FD opened. Balance: " + money(account.balance));
    }

    private static void createLoan() {
        Account account = BANK.find(readLong("Account number: "));
        BigDecimal principal = readMoney("Loan principal: ");
        BigDecimal annualRate = readMoney("Annual interest rate (%): ");
        int months = readInt("Tenure in months: ");
        account.createLoan(principal, annualRate, months);
        System.out.println("Loan created and amount credited. EMI: " + money(account.latestLoan().emi));
    }

    private static void payEmi() {
        Account account = BANK.find(readLong("Account number: "));
        account.payEmi();
        Loan loan = account.latestLoan();
        System.out.println("EMI paid. Balance: " + money(account.balance));
        System.out.println("Remaining loan amount: " + money(loan.outstanding));
    }

    private static void showAccount() {
        BANK.find(readLong("Account number: ")).printDetails();
    }

    private static void showHistory() {
        Account account = BANK.find(readLong("Account number: "));
        if (account.transactions.isEmpty()) {
            System.out.println("No transactions yet.");
            return;
        }
        account.transactions.forEach(System.out::println);
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return SCANNER.nextLine().trim();
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt));
            } catch (NumberFormatException ex) {
                System.out.println("Enter a valid whole number.");
            }
        }
    }

    private static long readLong(String prompt) {
        while (true) {
            try {
                return Long.parseLong(readLine(prompt));
            } catch (NumberFormatException ex) {
                System.out.println("Enter a valid account number.");
            }
        }
    }

    private static BigDecimal readMoney(String prompt) {
        while (true) {
            try {
                BigDecimal amount = new BigDecimal(readLine(prompt)).setScale(2, RoundingMode.HALF_UP);
                if (amount.compareTo(BigDecimal.ZERO) < 0) {
                    System.out.println("Amount cannot be negative.");
                    continue;
                }
                return amount;
            } catch (NumberFormatException ex) {
                System.out.println("Enter a valid amount.");
            }
        }
    }

    static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}

class Bank {
    private final Path dataFile;
    private final Map<Long, Account> accounts = new LinkedHashMap<>();
    private long nextAccountNumber = 100001;

    Bank(Path dataFile) {
        this.dataFile = dataFile;
    }

    long nextAccountNumber() {
        return nextAccountNumber++;
    }

    void addAccount(Account account) {
        accounts.put(account.accountNumber, account);
    }

    Account find(long accountNumber) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new BankException("Account not found.");
        }
        return account;
    }

    void listAccounts() {
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }
        accounts.values().stream()
                .sorted(Comparator.comparingLong(a -> a.accountNumber))
                .forEach(a -> System.out.printf("%d | %-20s | %-7s | Balance: %s%n",
                        a.accountNumber, a.holderName, a.type, BankingManagementSystem.money(a.balance)));
    }

    void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(dataFile)) {
            writer.write("NEXT|" + nextAccountNumber);
            writer.newLine();
            for (Account account : accounts.values()) {
                writer.write(account.serialize());
                writer.newLine();
            }
        } catch (IOException ex) {
            System.out.println("Could not save data: " + ex.getMessage());
        }
    }

    void load() {
        if (!Files.exists(dataFile)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(dataFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", -1);
                if (parts[0].equals("NEXT")) {
                    nextAccountNumber = Long.parseLong(parts[1]);
                } else if (parts[0].equals("ACCOUNT")) {
                    Account account = Account.deserialize(parts);
                    accounts.put(account.accountNumber, account);
                }
            }
        } catch (Exception ex) {
            System.out.println("Could not load previous data: " + ex.getMessage());
        }
    }
}

class Account {
    final long accountNumber;
    final String holderName;
    final String type;
    BigDecimal balance;
    BigDecimal annualInterestRate;
    BigDecimal maintenanceFee;
    final List<Transaction> transactions = new ArrayList<>();
    final List<FixedDeposit> fixedDeposits = new ArrayList<>();
    final List<Loan> loans = new ArrayList<>();

    private Account(long accountNumber, String holderName, String type, BigDecimal balance,
                    BigDecimal annualInterestRate, BigDecimal maintenanceFee) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.type = type;
        this.balance = balance;
        this.annualInterestRate = annualInterestRate;
        this.maintenanceFee = maintenanceFee;
        addTransaction("OPEN", balance, "Account opened");
    }

    static Account savings(long number, String holder, BigDecimal balance, BigDecimal rate) {
        return new Account(number, holder, "SAVING", balance, rate, BigDecimal.ZERO);
    }

    static Account current(long number, String holder, BigDecimal balance, BigDecimal fee) {
        return new Account(number, holder, "CURRENT", balance, BigDecimal.ZERO, fee);
    }

    void deposit(BigDecimal amount, String note) {
        requirePositive(amount);
        balance = balance.add(amount);
        addTransaction("CREDIT", amount, note);
    }

    void withdraw(BigDecimal amount, String note) {
        requirePositive(amount);
        if (amount.compareTo(balance) > 0) {
            throw new BankException("Insufficient balance.");
        }
        balance = balance.subtract(amount);
        addTransaction("DEBIT", amount, note);
    }

    void applyAccountRule() {
        if (type.equals("SAVING")) {
            BigDecimal interest = balance.multiply(annualInterestRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            balance = balance.add(interest);
            addTransaction("INTEREST", interest, "Yearly savings interest");
        } else {
            if (maintenanceFee.compareTo(balance) > 0) {
                throw new BankException("Maintenance fee cannot be deducted because balance is too low.");
            }
            balance = balance.subtract(maintenanceFee);
            addTransaction("FEE", maintenanceFee, "Current account maintenance fee");
        }
    }

    void openFixedDeposit(BigDecimal amount, BigDecimal annualRate, int months) {
        requirePositive(amount);
        if (months <= 0) {
            throw new BankException("FD duration must be positive.");
        }
        withdraw(amount, "Fixed deposit opened");
        fixedDeposits.add(new FixedDeposit(amount, annualRate, months));
    }

    void createLoan(BigDecimal principal, BigDecimal annualRate, int months) {
        requirePositive(principal);
        if (months <= 0) {
            throw new BankException("Loan tenure must be positive.");
        }
        Loan loan = new Loan(principal, annualRate, months);
        loans.add(loan);
        deposit(principal, "Loan disbursed");
    }

    void payEmi() {
        Loan loan = latestLoan();
        if (loan.outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Loan is already closed.");
        }
        BigDecimal amount = loan.emi.min(loan.outstanding);
        withdraw(amount, "EMI payment");
        loan.outstanding = loan.outstanding.subtract(amount).max(BigDecimal.ZERO);
        loan.paidInstallments++;
    }

    Loan latestLoan() {
        if (loans.isEmpty()) {
            throw new BankException("No loan exists for this account.");
        }
        return loans.get(loans.size() - 1);
    }

    void printDetails() {
        System.out.println("\nAccount number: " + accountNumber);
        System.out.println("Holder name   : " + holderName);
        System.out.println("Type          : " + type);
        System.out.println("Balance       : " + BankingManagementSystem.money(balance));
        System.out.println("FD count      : " + fixedDeposits.size());
        for (FixedDeposit fd : fixedDeposits) {
            System.out.println("  " + fd);
        }
        System.out.println("Loan count    : " + loans.size());
        for (Loan loan : loans) {
            System.out.println("  " + loan);
        }
    }

    void addTransaction(String type, BigDecimal amount, String note) {
        transactions.add(new Transaction(LocalDateTime.now(), type, amount, balance, note));
    }

    String serialize() {
        return String.join("|",
                "ACCOUNT",
                String.valueOf(accountNumber),
                encode(holderName),
                type,
                BankingManagementSystem.money(balance),
                BankingManagementSystem.money(annualInterestRate),
                BankingManagementSystem.money(maintenanceFee),
                encode(listToText(transactions)),
                encode(listToText(fixedDeposits)),
                encode(listToText(loans)));
    }

    static Account deserialize(String[] parts) {
        Account account = new Account(
                Long.parseLong(parts[1]),
                decode(parts[2]),
                parts[3],
                new BigDecimal(parts[4]),
                new BigDecimal(parts[5]),
                new BigDecimal(parts[6]));
        account.transactions.clear();
        for (String row : splitRows(decode(parts[7]))) {
            account.transactions.add(Transaction.fromData(row));
        }
        for (String row : splitRows(decode(parts[8]))) {
            account.fixedDeposits.add(FixedDeposit.fromData(row));
        }
        for (String row : splitRows(decode(parts[9]))) {
            account.loans.add(Loan.fromData(row));
        }
        return account;
    }

    private static String listToText(List<?> items) {
        List<String> rows = new ArrayList<>();
        for (Object item : items) {
            rows.add(item.toString());
        }
        return String.join("~", rows);
    }

    private static List<String> splitRows(String text) {
        if (text.isBlank()) {
            return List.of();
        }
        return List.of(text.split("~", -1));
    }

    private static String encode(String value) {
        return value.replace("%", "%25").replace("|", "%7C").replace("~", "%7E");
    }

    private static String decode(String value) {
        return value.replace("%7E", "~").replace("%7C", "|").replace("%25", "%");
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Amount must be greater than zero.");
        }
    }
}

class FixedDeposit {
    final BigDecimal principal;
    final BigDecimal annualRate;
    final int months;
    final LocalDate maturityDate;
    final BigDecimal maturityAmount;

    FixedDeposit(BigDecimal principal, BigDecimal annualRate, int months) {
        this(principal, annualRate, months, LocalDate.now().plusMonths(months),
                principal.add(principal.multiply(annualRate)
                        .multiply(BigDecimal.valueOf(months))
                        .divide(BigDecimal.valueOf(1200), 2, RoundingMode.HALF_UP)));
    }

    FixedDeposit(BigDecimal principal, BigDecimal annualRate, int months, LocalDate maturityDate, BigDecimal maturityAmount) {
        this.principal = principal;
        this.annualRate = annualRate;
        this.months = months;
        this.maturityDate = maturityDate;
        this.maturityAmount = maturityAmount;
    }

    static FixedDeposit fromData(String data) {
        String[] p = data.split(",", -1);
        return new FixedDeposit(new BigDecimal(p[0]), new BigDecimal(p[1]), Integer.parseInt(p[2]),
                LocalDate.parse(p[3]), new BigDecimal(p[4]));
    }

    public String toString() {
        return principal + "," + annualRate + "," + months + "," + maturityDate + "," + maturityAmount;
    }
}

class Loan {
    final BigDecimal principal;
    final BigDecimal annualRate;
    final int tenureMonths;
    final BigDecimal emi;
    BigDecimal outstanding;
    int paidInstallments;

    Loan(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        this(principal, annualRate, tenureMonths, calculateEmi(principal, annualRate, tenureMonths),
                calculateEmi(principal, annualRate, tenureMonths).multiply(BigDecimal.valueOf(tenureMonths)), 0);
    }

    Loan(BigDecimal principal, BigDecimal annualRate, int tenureMonths, BigDecimal emi, BigDecimal outstanding, int paidInstallments) {
        this.principal = principal;
        this.annualRate = annualRate;
        this.tenureMonths = tenureMonths;
        this.emi = emi;
        this.outstanding = outstanding;
        this.paidInstallments = paidInstallments;
    }

    static BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRate, int months) {
        double monthlyRate = annualRate.doubleValue() / 1200.0;
        if (monthlyRate == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        double p = principal.doubleValue();
        double emi = p * monthlyRate * Math.pow(1 + monthlyRate, months) / (Math.pow(1 + monthlyRate, months) - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    static Loan fromData(String data) {
        String[] p = data.split(",", -1);
        return new Loan(new BigDecimal(p[0]), new BigDecimal(p[1]), Integer.parseInt(p[2]),
                new BigDecimal(p[3]), new BigDecimal(p[4]), Integer.parseInt(p[5]));
    }

    public String toString() {
        return principal + "," + annualRate + "," + tenureMonths + "," + emi + "," + outstanding + "," + paidInstallments;
    }
}

class Transaction {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    final LocalDateTime dateTime;
    final String type;
    final BigDecimal amount;
    final BigDecimal balanceAfter;
    final String note;

    Transaction(LocalDateTime dateTime, String type, BigDecimal amount, BigDecimal balanceAfter, String note) {
        this.dateTime = dateTime;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.note = note;
    }

    static Transaction fromData(String data) {
        String[] p = data.split(",", 5);
        return new Transaction(LocalDateTime.parse(p[0], FORMATTER), p[1], new BigDecimal(p[2]), new BigDecimal(p[3]), p[4]);
    }

    public String toString() {
        return FORMATTER.format(dateTime) + "," + type + "," + amount + "," + balanceAfter + "," + note;
    }
}

class BankException extends RuntimeException {
    BankException(String message) {
        super(message);
    }
}