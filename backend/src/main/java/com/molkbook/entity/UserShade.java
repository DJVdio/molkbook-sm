package com.molkbook.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_shades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserShade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "shade_name", length = 100)
    private String shadeName;

    @Column(name = "shade_description", columnDefinition = "TEXT")
    private String shadeDescription;

    @Column(name = "confidence_level", length = 20)
    private String confidenceLevel;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
