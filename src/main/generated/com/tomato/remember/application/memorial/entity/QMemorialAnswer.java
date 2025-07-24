package com.tomato.remember.application.memorial.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemorialAnswer is a Querydsl query type for MemorialAnswer
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemorialAnswer extends EntityPathBase<MemorialAnswer> {

    private static final long serialVersionUID = -254601029L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemorialAnswer memorialAnswer = new QMemorialAnswer("memorialAnswer");

    public final com.tomato.remember.common.audit.QAudit _super = new com.tomato.remember.common.audit.QAudit(this);

    public final NumberPath<Integer> answerLength = createNumber("answerLength", Integer.class);

    public final StringPath answerText = createString("answerText");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> createdBy = _super.createdBy;

    public final com.tomato.remember.application.family.entity.QFamilyMember familyMember;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isComplete = createBoolean("isComplete");

    public final com.tomato.remember.application.member.entity.QMember member;

    public final QMemorial memorial;

    public final QMemorialQuestion question;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Long> updatedBy = _super.updatedBy;

    public QMemorialAnswer(String variable) {
        this(MemorialAnswer.class, forVariable(variable), INITS);
    }

    public QMemorialAnswer(Path<? extends MemorialAnswer> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemorialAnswer(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemorialAnswer(PathMetadata metadata, PathInits inits) {
        this(MemorialAnswer.class, metadata, inits);
    }

    public QMemorialAnswer(Class<? extends MemorialAnswer> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.familyMember = inits.isInitialized("familyMember") ? new com.tomato.remember.application.family.entity.QFamilyMember(forProperty("familyMember"), inits.get("familyMember")) : null;
        this.member = inits.isInitialized("member") ? new com.tomato.remember.application.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
        this.memorial = inits.isInitialized("memorial") ? new QMemorial(forProperty("memorial"), inits.get("memorial")) : null;
        this.question = inits.isInitialized("question") ? new QMemorialQuestion(forProperty("question")) : null;
    }

}

