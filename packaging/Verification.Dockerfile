FROM debian:bookworm-slim
# Model the standard Debian libraries supplied by the ChromeOS Linux environment.
# Deliberately no system Java, MongoDB, Docker daemon, or application data.
RUN apt-get update && apt-get install -y --no-install-recommends python3 procps libasound2 libbsd0 libx11-6 libxau6 libxcb1 libxdmcp6 libxext6 libxi6 libxrender1 libxtst6 xdg-utils && rm -rf /var/lib/apt/lists/*
RUN useradd --create-home agenda
CMD ["sleep", "infinity"]
