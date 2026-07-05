package com.finguard.controller;

import com.finguard.dto.AlertReviewRequest;
import com.finguard.model.AlertStatus;
import com.finguard.model.FraudAlert;
import com.finguard.repository.FraudAlertRepository;
import com.finguard.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    @Autowired
    private FraudAlertRepository fraudAlertRepository;

    @GetMapping
    public ResponseEntity<List<FraudAlert>> recent(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(fraudAlertRepository.findTop50ByOrderByCreatedAtDesc());
        }
        return ResponseEntity.ok(fraudAlertRepository.findTop50ByUsernameOrderByCreatedAtDesc(authentication.getName()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/review")
    public ResponseEntity<FraudAlert> review(@PathVariable Long id, @Valid @RequestBody AlertReviewRequest request) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + id));
        alert.setStatus(AlertStatus.valueOf(request.getStatus().toUpperCase()));
        return ResponseEntity.ok(fraudAlertRepository.save(alert));
    }
}
