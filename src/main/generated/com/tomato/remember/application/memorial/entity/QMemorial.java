package com.tomato.remember.application.memorial.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemorial is a Querydsl query type for Memorial
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemorial extends EntityPathBase<Memorial> {

    private static final long serialVersionUID = -858122595L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemorial memorial = new QMemorial("memorial");

    public final com.tomato.remember.common.audit.QAudit _super = new com.tomato.remember.common.audit.QAudit(this);

    public final BooleanPath aiTrainingCompleted = createBoolean("aiTrainingCompleted");

    public final EnumPath<com.tomato.remember.application.memorial.code.AiTrainingStatus> aiTrainingStatus = createEnum("aiTrainingStatus", com.tomato.remember.application.memorial.code.AiTrainingStatus.class);

    public final DatePath<java.time.LocalDate> birthDate = createDate("birthDate", java.time.LocalDate.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> createdBy = _super.createdBy;

    public final DatePath<java.time.LocalDate> deathDate = createDate("deathDate", java.time.LocalDate.class);

    public final StringPath description = createString("description");

    public final ListPath<com.tomato.remember.application.family.entity.FamilyMember, com.tomato.remember.application.family.entity.QFamilyMember> familyMembers = this.<com.tomato.remember.application.family.entity.FamilyMember, com.tomato.remember.application.family.entity.QFamilyMember>createList("familyMembers", com.tomato.remember.application.family.entity.FamilyMember.class, com.tomato.remember.application.family.entity.QFamilyMember.class, PathInits.DIRECT2);

    public final StringPath favoriteFood = createString("favoriteFood");

    public final EnumPath<com.tomato.remember.application.member.code.Gender> gender = createEnum("gender", com.tomato.remember.application.member.code.Gender.class);

    public final StringPath hobbies = createString("hobbies");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isPublic = createBoolean("isPublic");

    public final DatePath<java.time.LocalDate> lastVisitAt = createDate("lastVisitAt", java.time.LocalDate.class);

    public final ListPath<MemorialFile, QMemorialFile> memorialFiles = this.<MemorialFile, QMemorialFile>createList("memorialFiles", MemorialFile.class, QMemorialFile.class, PathInits.DIRECT2);

    public final NumberPath<Integer> memoryCount = createNumber("memoryCount", Integer.class);

    public final StringPath name = createString("name");

    public final StringPath nickname = createString("nickname");

    public final com.tomato.remember.application.member.entity.QMember owner;

    public final StringPath personality = createString("personality");

    public final EnumPath<com.tomato.remember.application.member.code.Relationship> relationship = createEnum("relationship", com.tomato.remember.application.member.code.Relationship.class);

    public final StringPath specialMemories = createString("specialMemories");

    public final StringPath speechHabits = createString("speechHabits");

    public final EnumPath<com.tomato.remember.application.memorial.code.MemorialStatus> status = createEnum("status", com.tomato.remember.application.memorial.code.MemorialStatus.class);

    public final NumberPath<Integer> totalVisits = createNumber("totalVisits", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Long> updatedBy = _super.updatedBy;

    public final ListPath<com.tomato.remember.application.wsvideo.entity.VideoCall, com.tomato.remember.application.wsvideo.entity.QVideoCall> videoCalls = this.<com.tomato.remember.application.wsvideo.entity.VideoCall, com.tomato.remember.application.wsvideo.entity.QVideoCall>createList("videoCalls", com.tomato.remember.application.wsvideo.entity.VideoCall.class, com.tomato.remember.application.wsvideo.entity.QVideoCall.class, PathInits.DIRECT2);

    public QMemorial(String variable) {
        this(Memorial.class, forVariable(variable), INITS);
    }

    public QMemorial(Path<? extends Memorial> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemorial(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemorial(PathMetadata metadata, PathInits inits) {
        this(Memorial.class, metadata, inits);
    }

    public QMemorial(Class<? extends Memorial> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.owner = inits.isInitialized("owner") ? new com.tomato.remember.application.member.entity.QMember(forProperty("owner"), inits.get("owner")) : null;
    }

}

