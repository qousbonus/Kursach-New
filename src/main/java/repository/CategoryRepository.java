package repository;

import model.Category;
import util.Json;
import util.SupabaseClient;

import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {

    private final SupabaseClient db = SupabaseClient.getInstance();

    private Category map(String obj) {
        Category c = new Category();
        c.setId(Json.lng(obj, "id"));
        c.setName(Json.str(obj, "name"));
        c.setDescription(Json.str(obj, "description"));
        c.setIcon(Json.str(obj, "icon"));
        return c;
    }

    public List<Category> findAll() throws Exception {
        String json = db.get("categories", "order=name.asc");
        List<Category> list = new ArrayList<>();
        for (String row : Json.array(json)) list.add(map(row));
        return list;
    }
}
