const express = require("express");
const path = require("path");

const app = express();

// public 폴더 안의 정적 파일 전부 제공
app.use(express.static(path.join(__dirname, "public")));

// 루트 요청 → index.html 열기
app.get("/", (req, res) => {
  res.sendFile(path.join(__dirname, "public/pages/post/post.html"));
});

app.get("/login", (req, res) => {
  res.sendFile(path.join(__dirname, "public/pages/login/login.html"));
});

app.get("/user/join/agree", (req, res) => {
  res.sendFile(path.join(__dirname, "public/pages/join/agree.html"));
});

app.get("/user/join/register", (req, res) => {
  res.sendFile(path.join(__dirname, "public/pages/join/register.html"));
});

app.listen(3000, () => {
  console.log("Frontend running at http://localhost:3000");
});
