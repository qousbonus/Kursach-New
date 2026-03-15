package service;

import model.User;
import repository.LogRepository;
import repository.UserRepository;
import util.Session;
import util.SupabaseAuth;

public class AuthService {

    private final UserRepository userRepo = new UserRepository();
    private final LogRepository logRepo = new LogRepository();
    private final SupabaseAuth sbAuth = new SupabaseAuth();

    public String login(String usernameOrEmail, String password) {
        try {
            User user = userRepo.findByUsername(usernameOrEmail);
            if (user == null) user = userRepo.findByEmail(usernameOrEmail);

            if (user == null) {
                userRepo.logFailedAttempt(usernameOrEmail, "User not found");
                return "Пользователь не найден";
            }
            if (user.isAccountLocked()) return "Аккаунт заблокирован. Обратитесь к администратору.";
            if (!user.isActive()) return "Аккаунт деактивирован.";

            SupabaseAuth.AuthResult res = sbAuth.signIn(user.getEmail(), password);
            if (!res.success) {
                userRepo.logFailedAttempt(user.getEmail(), "Wrong password");
                return res.error;
            }

            userRepo.updateLastLogin(user.getId());
            Session.getInstance().login(user);
            Session.getInstance().loadCartFromDb();
            logRepo.log(user.getId(), "LOGIN", "users", "Logged in: " + user.getUsername());
            return null;

        } catch (Exception e) {
            return "Ошибка подключения: " + e.getMessage();
        }
    }

    public String register(String username, String email, String password) {
        if (username == null || username.trim().length() < 3)
            return "Логин должен содержать минимум 3 символа";
        if (email == null || !email.contains("@"))
            return "Введите корректный email";
        if (password == null || password.length() < 6)
            return "Пароль должен содержать минимум 6 символов";

        try {
            if (userRepo.findByUsername(username.trim()) != null)
                return "Пользователь с таким логином уже существует";
            if (userRepo.findByEmail(email.trim()) != null)
                return "Email уже используется";

            SupabaseAuth.AuthResult res = sbAuth.signUp(email.trim().toLowerCase(), password);
            if (!res.success) return res.error;

            User u = new User();
            u.setUsername(username.trim());
            u.setEmail(email.trim().toLowerCase());
            u.setRole("user");
            u.setActive(true);
            userRepo.save(u);
            return null;

        } catch (Exception e) {
            return "Ошибка регистрации: " + e.getMessage();
        }
    }

    public void logout() {
        Session s = Session.getInstance();
        if (s.isLoggedIn())
            logRepo.log(s.getCurrentUser().getId(), "LOGOUT", "users", "Logged out: " + s.getCurrentUser().getUsername());
        s.logout();
    }
}
