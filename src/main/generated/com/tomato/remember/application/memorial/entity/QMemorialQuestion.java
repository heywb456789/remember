package com.tomato.remember.application.memorial.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemorialQuestion is a Querydsl query type for MemorialQuestion
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemorialQuestion extends EntityPathBase<MemorialQuestion> {

    private static final long serialVersionUID = -524760669L;

    public static final QMemorialQuestion memorialQuestion = new QMemorialQuestion("memorialQuestion");

    public final com.tomato.remember.common.audit.QAudit _super = new com.tomato.remember.common.audit.QAudit(this);

    public final ListPath<MemorialAnswer, QMemorialAnswer> answers = this.<MemorialAnswer, QMemorialAnswer>createList("answers", MemorialAnswer.class, QMemorialAnswer.class, PathInits.DIRECT2);

    public final StringPath category = createString("category");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> createdBy = _super.createdBy;

    public final StringPath description = createString("description");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isActive = createBoolean("isActive");

    public final BooleanPath isRequired = createBoolean("isRequired");

    public final NumberPath<Integer> maxLength = createNumber("maxLength", Integer.class);

    public final NumberPath<Integer> minLength = createNumber("minLength", Integer.class);

    public final StringPath placeholderText = createString("placeholderText");

    public final StringPath questionText = createString("questionText");

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final NumberPath<Long> updatedBy = _super.updatedBy;

    public QMemorialQuestion(String variable) {
        super(MemorialQuestion.class, forVariable(variable));
    }

    public QMemorialQuestion(Path<? extends MemorialQuestion> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMemorialQuestion(PathMetadata metadata) {
        super(MemorialQuestion.class, metadata);
    }

}

