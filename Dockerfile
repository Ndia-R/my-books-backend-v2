FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && \
    apt-get install -y git curl sudo bash python3 python3-pip && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

# Claude Codeをrootでインストール
RUN npm install -g @anthropic-ai/claude-code

# 既存ユーザーがいないので、新規作成
RUN useradd -m vscode

# rootユーザーで作業ディレクトリの所有者とグループを変更
USER root
WORKDIR /workspace
RUN chown vscode:vscode /workspace

# vscode​ユーザーに切り替えてからuvをインストール
USER vscode

# Python uvをvscode​ユーザーでインストール（Serena MCP用）
RUN curl -LsSf https://astral.sh/uv/install.sh | sh

# vscode​ユーザーのPATHにuvを追加
ENV PATH="/home/vscode/.local/bin:$PATH"
RUN echo 'export PATH="/home/vscode/.local/bin:$PATH"' >> /home/vscode/.bashrc