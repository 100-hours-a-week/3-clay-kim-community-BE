document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("loginForm");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value.trim();

    if (!email || !password) {
      alert("이메일과 비밀번호를 입력해주세요.");
      return;
    }

    try {
      const response = await fetch("http://localhost:8080/auth", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      if (!response.ok) throw new Error("로그인 실패");

      // 응답 본문에서 데이터 추출
      const result = await response.json();
      console.log("로그인 응답:", result);

      const accessToken = result.data.accessToken;
      
      // localStorage에 토큰과 이메일 저장
      if (accessToken) {
        localStorage.setItem("accessToken", accessToken);
        localStorage.setItem("userEmail", email);
      }

      console.log("로그인 성공");

      alert("로그인 성공!");
      window.location.href = "/pages/post/post.html"; // 로그인 성공 시 이동

    } catch (err) {
      console.error(err);
    //   alert(err);
      console.error("아이디와 비밀번호를 확인해주세요.");
    }
  });
});
