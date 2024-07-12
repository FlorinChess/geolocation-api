## Geolocation API

RESTful web API developed in Java using Spring Boot and gRPC. This project contains three programs:

`backend`: Java console application that queries data from the openstreetmap database and hosts the gRPC server

`middleware`: Spring Boot web API that forwards data from the backend to the user based on the GET request; returns `.json` responses 

`frontend`: Static webpage for testing the API endpoints 



## Features

- query amenity information based on type, coordinates, perimeter or openstreetmap ID
- query road/street/highway information based on coordinates, perimeter or openstreetmap ID
- query land usage information with a specified area
- routing between two specified locations based on specified weighting (length or time)
- draw tiles for graphical representation of maps



## API Endpoints


### `GET  /amenities`

This request returns amenities within the specified area. The specified area is defined either by a perimeter or a circle drawn around a given point. 

**Parameters:**
- `amenity`: optional, can specify the type of amenity to return, if empty: return all types
- `bbox.tl.x`,`bbox.tl.y`: top-left of the bounding box to search for
- `bbox.br.x`,`bbox.br.y`: bottom-right of the bounding box to search for
- `point.x`, `point.y`: center of the point
- `point.d`: maximum distance to the point, in meters
- `take` (default: 50) 
- `skip` (default: 0): optional parameters for paging, take is the limit, skip is how many you need to step over.


### `GET /amenities/{id}`

This request returns information about the amenity with the given openstreetmap ID specified by the path parameter `id`. 

**Parameters**: none


### `GET /roads`

This request returns roads within the specified area.

**Parameters:**
- `road`: optional, can specify the type of road to return, if empty: return all types
- `bbox.tl.x`, `bbox.tl.y`: top-left of the bounding box to search for
- `bbox.br.x`, `bbox.br.y`: bottom-right of the bounding box to search for
- `take` (default: 50)
- `skip` (default: 0): optional parameters for paging, take is the limit, skip is how many you need to step over.


### `GET /roads/{id}`

This request returns information about the road with the given openstreetmap ID specified by the path parameter `id`.

**Parameters:** None


### `GET /tile/{z}/{x}/{y}.png`

This request returns map tiles as 512 x 512 PNG images for a given map segment as indicated by the path parameters `x`, `y` and `z`.

**Parameters:**

- `layers` (default: `motorway`): comma-separated list of layers to display, these should be drawn in the order specified (the last one is drawn last)

**Layers:**

- `motorway`, `trunk`, `primary`, `secondary`, `road` 
- `forest`, `residential`, `vineyard`, `grass`, `railway`
- `water` (any entity with the key `water`)


### `GET /route`

This request generates a route from the node with ID `from` to the node with ID `to`.

**Parameters:**
- `from`: start node ID
- `to`: end node ID
- `weighting`: either `time` or `length`, default: `length`


### `GET /usage`

This request returns land usage statistics based on the provided perimeter.

**Parameters:**
- `bbox.tl.x`, `bbox.tl.y`: top-left of the bounding box to calculate the usage for
- `bbox.br.x`, `bbox.br.y`: bottom-right of the bounding box to calculate the usage for



## Third-party libraries and APIs

- [Lombok](https://projectlombok.org/)
- [gRPC](https://grpc.io/)
- [openstreetmap.org](https://www.openstreetmap.org)

## Disclaimer

This project was built in collaboration with Elena Balent and Michelle Balcos as part of a university course.