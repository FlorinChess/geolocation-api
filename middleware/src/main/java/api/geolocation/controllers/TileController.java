package api.geolocation.controllers;

import api.geolocation.MapApplication;
import api.geolocation.Status;
import api.geolocation.TileRequest;
import api.geolocation.exceptions.InvalidRequestException;
import api.geolocation.exceptions.NotFoundException;
import com.google.protobuf.ByteString;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tile")
public class TileController {
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @GetMapping("/{z}/{x}/{y}.png")
    public ResponseEntity<Object> getTile(
            @PathVariable int z,
            @PathVariable int x,
            @PathVariable int y,
            @RequestParam(required = false) String layers) {
        var byteString = loadTile(z, x, y, layers);

        byte[] pngBytes = new byte[byteString.size()];

        byteString.copyTo(pngBytes, 0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<>(pngBytes, headers, HttpStatus.OK);
    }

    private ByteString loadTile(int z, int x, int y, String layers) {
        var request = TileRequest.newBuilder()
                .setZ(z)
                .setX(x)
                .setY(y)
                .setLayers(layers)
                .build();

        var response = MapApplication.stub.getTile(request);

        if (response.getStatus() == Status.Success) {
            return response.getPng();
        }

        if (response.getStatus() == Status.NotFound) {
            throw new NotFoundException("Tile not found");
        }

        if (response.getStatus() == Status.InternalError) {
            throw new InvalidRequestException("Tile request invalid!");
        }

        return ByteString.EMPTY;
    }
}
