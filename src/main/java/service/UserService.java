package service;

import model.User;
import repository.LogRepository;
import repository.UserRepository;
import util.Session;

import java.util.List;

public class UserService {

    private final UserRepository userRepo = new UserRepository();
    private final LogRepository logRepo = new LogRepository();

    public List<User> getAll() {
        try {
            return userRepo.findAll();
        } catch (Exception e) {
            return List.of();
        }
    }

    public User getById(long id) {
        try {
            return userRepo.findById(id);
        } catch (Exception e) {
            return null;
        }
    }

    public String updateProfile(User u) {
        try {
            userRepo.updateProfile(u);
            return null;
        } catch (Exception e) {
            return "Ошибка обновления профиля: " + e.getMessage();
        }
    }

    public String updateEmail(User u) {
        try {
            userRepo.updateEmail(u.getId(), u.getEmail());
            return null;
        } catch (Exception e) {
            return "Ошибка обновления email: " + e.getMessage();
        }
    }

    public String blockUser(long userId) {
        try {
            userRepo.setLocked(userId, true);
            logAction("USER_BLOCKED", "Blocked #" + userId);
            return null;
        } catch (Exception e) {
            return "Ошибка блокировки: " + e.getMessage();
        }
    }

    public String unblockUser(long userId) {
        try {
            userRepo.setLocked(userId, false);
            logAction("USER_UNBLOCKED", "Unblocked #" + userId);
            return null;
        } catch (Exception e) {
            return "Ошибка разблокировки: " + e.getMessage();
        }
    }

    public String deactivate(long userId) {
        try {
            userRepo.setActive(userId, false);
            logAction("USER_DEACTIVATED", "Deactivated #" + userId);
            return null;
        } catch (Exception e) {
            return "Ошибка деактивации: " + e.getMessage();
        }
    }

    private void logAction(String action, String details) {
        Session s = Session.getInstance();
        if (s.isLoggedIn()) logRepo.log(s.getCurrentUser().getId(), action, "users", details);
    }

    public List<Object[]> getFailedAttempts() {
        try {
            return userRepo.getFailedAttempts();
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Object[]> getLogs() {
        try {
            return new LogRepository().findAll();
        } catch (Exception e) {
            return List.of();
        }
    }
}
