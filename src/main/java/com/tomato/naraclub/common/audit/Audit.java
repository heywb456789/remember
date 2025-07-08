package com.tomato.naraclub.common.audit;

import com.tomato.naraclub.common.dto.AuditDTO;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@SuperBuilder
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
public abstract class Audit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  protected Long id;

  @Comment("등록자")
  @CreatedBy
  @Column(updatable = false, length = 100)
  protected Long createdBy;

  @Comment("등록일시")
  @CreatedDate
  @Column(updatable = false)
  @Builder.Default
  protected LocalDateTime createdAt = LocalDateTime.now();

  @Comment("수정자")
  @LastModifiedBy
  @Column(length = 100)
  protected Long updatedBy;

  @Comment("수정일시")
  @LastModifiedDate
  @Column
  @Builder.Default
  protected LocalDateTime updatedAt = LocalDateTime.now();

  public <T extends AuditDTO> T auditDTO(T aa) {
    aa.setAudit(this);
    return aa;
  }

}
