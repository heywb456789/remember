package com.tomato.naraclub.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tomato.naraclub.common.audit.Audit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
public class AuditDTO {

  @Schema(description = "Entity PK")
  private Long id;

  @Schema(description = "등록자")
  private Long createdBy;

  @Schema(description = "등록일시")
  private LocalDateTime createdAt;

  @Schema(description = "수정자")
  private Long updatedBy;

  @Schema(description = "수정일시")
  private LocalDateTime updatedAt;

  @JsonIgnore
  public <T extends Audit, R extends AuditDTO> R setAudit(T entity) {
    id = entity.getId();
    createdBy = entity.getCreatedBy();
    createdAt = entity.getCreatedAt();
    updatedBy = entity.getUpdatedBy();
    updatedAt = entity.getUpdatedAt();
    return (R) this;
  }


}
