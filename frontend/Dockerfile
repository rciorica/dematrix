FROM node:20-alpine as build

WORKDIR /app

COPY package*.json ./

RUN npm install --legacy-peer-deps

COPY public ./public
COPY src ./src

# DO NOT set REACT_APP_API_URL here - let runtime detection handle it
# The app will auto-detect localhost:8080 in the browser

RUN npm run build

# Serve with simple HTTP server
FROM node:20-alpine

WORKDIR /app

RUN npm install -g serve

COPY --from=build /app/build ./build

ENV PORT=3000

EXPOSE 3000

CMD ["serve", "-s", "build", "-l", "3000"]
