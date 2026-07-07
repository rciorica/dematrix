FROM node:20-alpine as build

WORKDIR /app

COPY package*.json ./

RUN npm install --legacy-peer-deps

COPY .env .env
COPY public ./public
COPY src ./src

ENV REACT_APP_API_URL=http://localhost:8080/api

RUN npm run build

# Serve with simple HTTP server
FROM node:20-alpine

WORKDIR /app

RUN npm install -g serve

COPY --from=build /app/build ./build

ENV PORT=3000

EXPOSE 3000

CMD ["serve", "-s", "build", "-l", "3000"]
