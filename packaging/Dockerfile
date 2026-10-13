FROM debian:bookworm-slim
ARG JDK_URL
ARG JDK_SHA256
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl fakeroot binutils dpkg-dev libstdc++6 zlib1g libasound2 libx11-6 libxext6 libxi6 libxrender1 libxtst6 libfreetype6 libfontconfig1 && rm -rf /var/lib/apt/lists/*
RUN test -n "$JDK_URL" && test -n "$JDK_SHA256" && curl --fail --location --retry 3 "$JDK_URL" -o /tmp/jdk.tar.gz && echo "$JDK_SHA256  /tmp/jdk.tar.gz" | sha256sum -c - && mkdir /opt/jdk && tar -xzf /tmp/jdk.tar.gz -C /opt/jdk --strip-components=1 && rm /tmp/jdk.tar.gz
COPY agenda-wa.jar agenda-wa.png /input/
RUN /opt/jdk/bin/jpackage --type deb --name "Agenda WA" --linux-package-name agenda-wa --app-version 1.0.0 --vendor "Agenda WA" --description "Agenda locale per clienti, appuntamenti e callback" --input /input --main-jar agenda-wa.jar --main-class org.springframework.boot.loader.launch.JarLauncher --icon /input/agenda-wa.png --linux-shortcut --linux-menu-group Office --linux-app-category Office --install-dir /opt --java-options -Xmx384m --java-options -Djava.awt.headless=true --dest /out
# xdg-desktop-menu also needs this standard directory on minimal Debian installs.
# Include it in the package, preserving jpackage's native launcher and maintainer files.
RUN dpkg-deb --raw-extract /out/agenda-wa_1.0.0_*.deb /tmp/package && mkdir -p /tmp/package/usr/share/desktop-directories && dpkg-deb --build --root-owner-group /tmp/package /tmp/agenda-wa.deb && mv /tmp/agenda-wa.deb /out/$(basename /out/agenda-wa_1.0.0_*.deb) && rm -rf /tmp/package
