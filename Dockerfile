FROM openjdk:19
WORKDIR /app
COPY . .
RUN mkdir -p out && javac -d out src/Main.java
RUN mkdir -p public && cp src/*.html src/*.css src/*.js public/
WORKDIR /app/public
CMD ["java", "-cp", "../out", "Main"]