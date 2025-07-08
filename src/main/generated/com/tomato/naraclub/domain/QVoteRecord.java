package com.tomato.naraclub.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QVoteRecord is a Querydsl query type for VoteRecord
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QVoteRecord extends EntityPathBase<VoteRecord> {

    private static final long serialVersionUID = -774243066L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QVoteRecord voteRecord = new QVoteRecord("voteRecord");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.tomato.naraclub.application.member.entity.QMember member;

    public final StringPath selected = createString("selected");

    public final DateTimePath<java.time.LocalDateTime> votedAt = createDateTime("votedAt", java.time.LocalDateTime.class);

    public final QVotePost votePost;

    public QVoteRecord(String variable) {
        this(VoteRecord.class, forVariable(variable), INITS);
    }

    public QVoteRecord(Path<? extends VoteRecord> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QVoteRecord(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QVoteRecord(PathMetadata metadata, PathInits inits) {
        this(VoteRecord.class, metadata, inits);
    }

    public QVoteRecord(Class<? extends VoteRecord> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new com.tomato.naraclub.application.member.entity.QMember(forProperty("member"), inits.get("member")) : null;
        this.votePost = inits.isInitialized("votePost") ? new QVotePost(forProperty("votePost"), inits.get("votePost")) : null;
    }

}

