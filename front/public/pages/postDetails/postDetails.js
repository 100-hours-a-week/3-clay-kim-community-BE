document.addEventListener("DOMContentLoaded", async () => {
    const title = document.getElementById("postTitle");
    const nickname = document.getElementById("postNickname");
    const content = document.getElementById("postContent");
    
    const viewCount = document.getElementById("viewCount");
    const likeCount = document.getElementById("likeCount");
    const commentCount = document.getElementById("commentCount");

    const urlParams = new URLSearchParams(window.location.search);
    const postId = urlParams.get("id");

    try {
        // api -- api list
        // api server config fetch method overriding
        // api list > 
        // fetchApi(GET_USERS) =>? () { error, result json }
        const response = await fetch(`http://localhost:8080/posts/${postId}`);
        console.log(postId);

        if (!response.ok) throw new Error("게시글을 불러오지 못했습니다.");

        const post = await response.json();
        console.log(post);

        title.textContent = post.data.title;
        nickname.textContent = post.data.nickname;
        content.textContent = post.data.content;

        
        const statusResponse = await fetch(`http://localhost:8080/posts/${postId}/statuses`);
        const status = await statusResponse.json();

        console.log(status);

        viewCount.textContent = status.data.viewCount;
        likeCount.textContent = status.data.likeCount;
        commentCount.textContent = status.data.commentCount;

    } catch(err) {
        console.error(err);
        alert("게시글을 불러오는 중 오류가 발생했습니다.");
    }
});