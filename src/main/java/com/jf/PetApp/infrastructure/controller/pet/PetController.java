package com.jf.PetApp.infrastructure.controller.pet;

import com.jf.PetApp.application.pet.usecase.ConfigurePetUseCase;
import com.jf.PetApp.application.pet.usecase.GetPetStatusUseCase;
import com.jf.PetApp.application.pet.usecase.GetMyPetUseCase;
import com.jf.PetApp.application.pet.usecase.LinkPetToAppUseCase;
import com.jf.PetApp.application.pet.usecase.ListMyPetsUseCase;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import com.jf.PetApp.core.domain.enums.PetSpecieEnum;
import com.jf.PetApp.core.security.SecurityUtils;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final ConfigurePetUseCase configurePetUseCase;
    private final GetPetStatusUseCase getPetStatusUseCase;
    private final GetMyPetUseCase getMyPetUseCase;
    private final LinkPetToAppUseCase linkPetToAppUseCase;
    private final ListMyPetsUseCase listMyPetsUseCase;

    public PetController(
            ConfigurePetUseCase configurePetUseCase,
            GetPetStatusUseCase getPetStatusUseCase,
            GetMyPetUseCase getMyPetUseCase,
            LinkPetToAppUseCase linkPetToAppUseCase,
            ListMyPetsUseCase listMyPetsUseCase) {
        this.configurePetUseCase = configurePetUseCase;
        this.getPetStatusUseCase = getPetStatusUseCase;
        this.getMyPetUseCase = getMyPetUseCase;
        this.linkPetToAppUseCase = linkPetToAppUseCase;
        this.listMyPetsUseCase = listMyPetsUseCase;
    }

    @PostMapping("/configure")
    public ResponseEntity<Void> configurePet(@RequestBody ConfigurePetRequestDTO request) {
        String email = SecurityUtils.getCurrentUserEmail();
        AppContextEnum appContext = SecurityUtils.getCurrentAppContext()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "appContext required"));
        try {
            PetSpecieEnum specie = PetSpecieEnum.valueOf(request.specie().toUpperCase());
            if (specie != appContext.defaultPetSpecie()) {
                throw new IllegalArgumentException("Specie not allowed for this app");
            }
            configurePetUseCase.execute(email, appContext, specie, request.name());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid specie");
        }
    }

    /** Points the caller's current app at a pet they already own — e.g. reusing their Wallet dog in Health too. */
    @PostMapping("/{petId}/link")
    public ResponseEntity<Void> linkPetToCurrentApp(@PathVariable Integer petId) {
        String email = SecurityUtils.getCurrentUserEmail();
        AppContextEnum appContext = SecurityUtils.getCurrentAppContext()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "appContext required"));
        try {
            linkPetToAppUseCase.execute(email, petId, appContext);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** Every pet the caller owns, regardless of which app(s) it currently answers for. */
    @GetMapping
    public ResponseEntity<List<PetSummaryResponseDTO>> listMyPets() {
        String email = SecurityUtils.getCurrentUserEmail();
        List<PetSummaryResponseDTO> body = listMyPetsUseCase.execute(email).stream()
                .map(pet -> new PetSummaryResponseDTO(pet.getId(), pet.getSpecie().name(), pet.getName(), pet.getHealth()))
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/status")
    public ResponseEntity<PetStatusResponseDTO> getStatus() {
        String email = SecurityUtils.getCurrentUserEmail();
        Optional<AppContextEnum> appContext = SecurityUtils.getCurrentAppContext();
        boolean hasPet = appContext.isPresent() && getPetStatusUseCase.execute(email, appContext.get());
        return ResponseEntity.ok(new PetStatusResponseDTO(hasPet));
    }

    @GetMapping("/my-pet")
    public ResponseEntity<PetDetailResponseDTO> getMyPet() {
        String email = SecurityUtils.getCurrentUserEmail();
        Optional<AppContextEnum> appContext = SecurityUtils.getCurrentAppContext();
        Optional<Pet> petOpt = appContext.flatMap(ctx -> getMyPetUseCase.execute(email, ctx));
        if (petOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Pet pet = petOpt.get();
        return ResponseEntity.ok(new PetDetailResponseDTO(pet.getSpecie().name(), pet.getName(), pet.getHealth()));
    }

    public record ConfigurePetRequestDTO(String specie, String name) {}
    public record PetStatusResponseDTO(boolean hasPet) {}
    public record PetDetailResponseDTO(String specie, String name, int health) {}
    public record PetSummaryResponseDTO(Integer id, String specie, String name, int health) {}
}
