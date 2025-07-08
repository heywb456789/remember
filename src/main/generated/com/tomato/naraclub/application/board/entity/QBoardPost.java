package com.tomato.naraclub.application.board.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBoardPost is a Querydsl query type for BoardPost
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoardPost extends EntityPathBase<BoardPost> {

    private static final long serialVersionUID = 935987488L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBoardPost boardPost = new QBoardPost("boardPost");

    public final com.tomato.naraclub.common.audit.QAudit _super = new com.tomato.naraclub.common.audit.QAudit(this);

    public final com.tomato.naraclub.application.member.entity.QMember author;

    public final NumberPath<Long> commentCount = createNumber("commentCount", Long.class);

    public final ListPath<com.tomato.naraclub.application.comment.entity.BoardComments, com.tomato.naraclub.application.comment.entity.QBoardComments> comments = this.<com.tomato.naraclub.application.comment.entity.BoardComments, com.tomato.naraclub.application.comment.entity.QBoardComments>createList("comments", com.tomato.naraclub.application.comment.entity.BoardComments.class, com.tomato.naraclub.application.comment.entity.QBoardComments.class, PathInits.DIRECT2);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final ListPath<BoardPostImage, QBoardPostImage> images = this.<BoardPostImage, QBoardPostImage>createList("images", BoardPostImage.class, QBoardPostImage.class, PathInits.DIRECT2);

    public final BooleanPath isHot = createBoolean("isHot");

    public final BooleanPath isNew = createBoolean("isNew");

    public final NumberPath<Integer> likes = createNumber("likes", Integer.class);

    public final NumberPath<Integer> shareCount = createNumber("shareCount", Integer.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public final NumberPath<Integer> views = createNumber("views", Integer.class);

    public QBoardPost(String variable) {
        this(BoardPost.class, forVariable(variable), INITS);
    }

    public QBoardPost(Path<? extends BoardPost> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBoardPost(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBoardPost(PathMetadata metadata, PathInits inits) {
        this(BoardPost.class, metadata, inits);
    }

    public QBoardPost(Class<? extends BoardPost> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.author = inits.isInitialized("author") ? new com.tomato.naraclub.application.member.entity.QMember(forProperty("author"), inits.get("author")) : null;
    }

}

