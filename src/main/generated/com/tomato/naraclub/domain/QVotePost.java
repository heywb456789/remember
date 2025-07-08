package com.tomato.naraclub.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QVotePost is a Querydsl query type for VotePost
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QVotePost extends EntityPathBase<VotePost> {

    private static final long serialVersionUID = -277949803L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QVotePost votePost = new QVotePost("votePost");

    public final com.tomato.naraclub.application.member.entity.QMember author;

    public final StringPath choiceA = createString("choiceA");

    public final StringPath choiceB = createString("choiceB");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath question = createString("question");

    public final NumberPath<Integer> voteCountA = createNumber("voteCountA", Integer.class);

    public final NumberPath<Integer> voteCountB = createNumber("voteCountB", Integer.class);

    public QVotePost(String variable) {
        this(VotePost.class, forVariable(variable), INITS);
    }

    public QVotePost(Path<? extends VotePost> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QVotePost(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QVotePost(PathMetadata metadata, PathInits inits) {
        this(VotePost.class, metadata, inits);
    }

    public QVotePost(Class<? extends VotePost> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.author = inits.isInitialized("author") ? new com.tomato.naraclub.application.member.entity.QMember(forProperty("author"), inits.get("author")) : null;
    }

}

