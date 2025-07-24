package com.tomato.remember.application.memorial.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemorialFile is a Querydsl query type for MemorialFile
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemorialFile extends EntityPathBase<MemorialFile> {

    private static final long serialVersionUID = 245688761L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemorialFile memorialFile = new QMemorialFile("memorialFile");

    public final com.tomato.remember.common.audit.QAudit _super = new com.tomato.remember.common.audit.QAudit(this);

    public final StringPath contentType = createString("contentType");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> createdBy = _super.createdBy;

    public final StringPath description = createString("description");

    public final NumberPath<Long> fileSize = createNumber("fileSize", Long.class);

    public final EnumPath<com.tomato.remember.application.memorial.code.MemorialFileType> fileType = createEnum("fileType", com.tomato.remember.application.memorial.code.MemorialFileType.class);

    public final StringPath fileUrl = createString("fileUrl");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QMemorial memorial;

    public final StringPath originalFilename = createString("originalFilename");

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Long> updatedBy = _super.updatedBy;

    public QMemorialFile(String variable) {
        this(MemorialFile.class, forVariable(variable), INITS);
    }

    public QMemorialFile(Path<? extends MemorialFile> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemorialFile(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemorialFile(PathMetadata metadata, PathInits inits) {
        this(MemorialFile.class, metadata, inits);
    }

    public QMemorialFile(Class<? extends MemorialFile> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.memorial = inits.isInitialized("memorial") ? new QMemorial(forProperty("memorial"), inits.get("memorial")) : null;
    }

}

