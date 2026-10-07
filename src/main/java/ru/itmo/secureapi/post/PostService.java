package ru.itmo.secureapi.post;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.itmo.secureapi.user.User;
import ru.itmo.secureapi.user.UserRepository;

@Service
public class PostService {

    private final PostRepository posts;
    private final UserRepository users;

    public PostService(PostRepository posts, UserRepository users) {
        this.posts = posts;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<PostResponse> list(String query) {
        List<Post> result = StringUtils.hasText(query) ? posts.search(query.trim()) : posts.findAllWithAuthor();
        return result.stream().map(PostResponse::from).toList();
    }

    @Transactional
    public PostResponse create(String username, CreatePostRequest request) {
        User author = users.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        Post post = posts.save(new Post(request.title().trim(), request.content().trim(), author));
        return PostResponse.from(post);
    }
}
