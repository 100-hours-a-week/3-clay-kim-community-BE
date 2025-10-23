//package kr.kakaotech.community.service;
//
//import kr.kakaotech.community.dto.response.PostListResponse;
//import kr.kakaotech.community.entity.Post;
//import kr.kakaotech.community.repository.PostRepository;
//import kr.kakaotech.community.repository.UserRepository;
//import org.assertj.core.api.Assertions;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//
//import java.awt.print.Pageable;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyInt;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(SpringExtension.class)
//class PostServiceTest {
//
//    @Mock
//    private PostRepository postRepository;
//    @Mock
//    private UserRepository userRepository;
//
//    @InjectMocks
//    private PostService postService;
//
//    private List<Post> getPosts() {
//        return List.of(
//                new Post(), new Post(), new Post()
//        );
//    }
//
//
//    @DisplayName("게시글 목록 조회 - 첫 조회, 다음 게시물 있음")
//    @Test
//    void getPostList_firstSearch_hasNextCursor() {
//        // given
//        Integer cursor = null;
//        Pageable pageable = new Pageable(3);
//        List<Post> posts = getPosts();
//
//        when(postRepository.findTopPost(size)).thenReturn(posts);
//
//        // when
//        PostListResponse postList = postService.getPostList(cursor, size);
//
//        // then
//        verify(postRepository, times(1)).findTopPost(size);
//        verify(postRepository, never()).findPostByCursor(anyInt(), anyInt());
//
//        assertThat(postList.getPosts().size()).isEqualTo(size);
//        assertThat(postList.isHasNext()).isTrue();
//    }
//
//    @DisplayName("게시글 목록 조회 - 두 번째 조회, 다음 게시물 없음")
//    @Test
//    void getPostList_secondSearch_nonHasNextCursor() {
//        // given
//        Integer cursor = 2;
//        int size = 5;
//        List<Post> posts = getPosts();
//
//        when(postRepository.findPostByCursor(cursor, size)).thenReturn(posts);
//
//        // when
//        PostListResponse postList = postService.getPostList(cursor, size);
//
//        // then
////        verify(postRepository, times(1)).findPostByCursor(cursor, size);
////        verify(postRepository, never()).findTopPost(anyInt());
//
//        assertThat(postList.getPosts().size()).isLessThan(size);
//        assertThat(postList.isHasNext()).isFalse();
//    }
//}