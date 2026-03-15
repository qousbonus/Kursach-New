package service;

import model.PromoCode;
import repository.PromoCodeRepository;

import java.util.List;

public class PromoService {

    private final PromoCodeRepository repo = new PromoCodeRepository();

    public List<PromoCode> getAll() {
        try {
            return repo.findAll();
        } catch (Exception e) {
            return List.of();
        }
    }

    public String save(PromoCode pc) {
        if (pc.getCode() == null || pc.getCode().isBlank())
            return "Введите код промокода";
        if (pc.getDiscountPercent() < 1 || pc.getDiscountPercent() > 100)
            return "Скидка должна быть от 1 до 100%";
        if (pc.getMaxUses() < 1)
            return "Укажите максимальное количество использований";
        try {
            if (pc.getId() == 0) repo.save(pc);
            else repo.update(pc);
            return null;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("23505") || msg.contains("duplicate"))
                return "Промокод с таким кодом уже существует";
            return "Ошибка сохранения: " + msg;
        }
    }

    public String delete(long id) {
        try {
            repo.delete(id);
            return null;
        } catch (Exception e) {
            return "Ошибка удаления: " + e.getMessage();
        }
    }

    public String toggle(long id, boolean active) {
        try {
            repo.setActive(id, active);
            return null;
        } catch (Exception e) {
            return "Ошибка: " + e.getMessage();
        }
    }
}
