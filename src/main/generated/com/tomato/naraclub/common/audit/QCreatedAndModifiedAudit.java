package com.tomato.naraclub.common.audit;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCreatedAndModifiedAudit is a Querydsl query type for CreatedAndModifiedAudit
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QCreatedAndModifiedAudit extends EntityPathBase<CreatedAndModifiedAudit> {

    private static final long serialVersionUID = -1971782708L;

    public static final QCreatedAndModifiedAudit createdAndModifiedAudit = new QCreatedAndModifiedAudit("createdAndModifiedAudit");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath createdBy = createString("createdBy");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final StringPath updatedBy = createString("updatedBy");

    public QCreatedAndModifiedAudit(String variable) {
        super(CreatedAndModifiedAudit.class, forVariable(variable));
    }

    public QCreatedAndModifiedAudit(Path<? extends CreatedAndModifiedAudit> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCreatedAndModifiedAudit(PathMetadata metadata) {
        super(CreatedAndModifiedAudit.class, metadata);
    }

}

