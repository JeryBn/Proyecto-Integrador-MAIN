package pe.tecsup.main;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CampusBrandController {
    private final Brand brand;

    public CampusBrandController(
            @Value("${MAIN_CAMPUS_NAME:Colegia Main}") String name,
            @Value("${MAIN_CAMPUS_COLOR:#087f68}") String color) {
        String displayName = name.strip();
        if (displayName.isEmpty() || displayName.length() > 80 ||
                displayName.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Nombre de campus inválido.");
        }
        if (!color.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("El color del campus debe tener formato #RRGGBB.");
        }
        this.brand = new Brand(displayName, color);
    }

    @GetMapping("/api/campus/brand")
    public Brand brand() {
        return brand;
    }

    public record Brand(String name, String color) {}
}
