import React from "react";

import "./Tile.css"

let apiUrl = "http://localhost:8010/tile/";

function Tile (x, y, zoom)
{
    // const imageData = fetchImage(x, y, zoom);

    return (
        <div className="tile">

            {/*<img src={imageData}></img>*/}
            <h2>x: {x}, y: {y}</h2>
        </div>
    );
}

function foo() {

}

export default Tile;