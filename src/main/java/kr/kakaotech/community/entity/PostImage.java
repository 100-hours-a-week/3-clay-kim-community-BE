package kr.kakaotech.community.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity(name = "post_image")
public class PostImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "image_id")
    private Image image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    public PostImage() {
    }

    public PostImage(Image image) {
        this.image = image;
    }

    public PostImage(Post post, Image image) {
        this.post = post;
        this.image = image;
    }
}
