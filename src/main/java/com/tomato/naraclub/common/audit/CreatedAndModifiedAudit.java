package com.tomato.naraclub.common.audit;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
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
public abstract class CreatedAndModifiedAudit {

  @Comment("등록자")
  @CreatedBy
  @Column(updatable = false)
  protected String createdBy;

  @Comment("등록일시")
  @CreatedDate
  @Column(updatable = false)
  protected LocalDateTime createdAt;

  @Comment("수정자")
  @LastModifiedBy
  @Column
  protected String updatedBy;

  @Comment("수정일시")
  @LastModifiedDate
  @Column
  protected LocalDateTime updatedAt;

}
