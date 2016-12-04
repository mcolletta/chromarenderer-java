package net.chromarenderer.utils;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import net.chromarenderer.main.ChromaLogger;
import net.chromarenderer.math.ImmutableMatrix3x3;
import net.chromarenderer.math.ImmutableVector3;
import net.chromarenderer.math.geometry.Geometry;
import net.chromarenderer.math.geometry.SimpleTriangle;
import net.chromarenderer.renderer.camera.Camera;
import net.chromarenderer.renderer.camera.PinholeCamera;
import net.chromarenderer.renderer.scene.GeometryScene;
import net.chromarenderer.renderer.shader.Material;
import net.chromarenderer.renderer.shader.MaterialType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author bensteinert
 */
public class BlenderChromaImporter {

    private static final String BLENDER_TO_CHROMA_SCRIPT = "blenderToChroma.py";
    private static final Logger LOGGER = ChromaLogger.get();


    public enum BlenderChromaConversionStatus {
        OK, FAIL, NOTHING_TODO;
    }


    public static BlenderChromaConversionStatus execBlenderToChromaConversion(Path path, String blenderFileName) {

        try {
            String sceneName = blenderFileName.substring(0, blenderFileName.lastIndexOf('.'));
            Path pythonScript = path.getParent().resolve(BLENDER_TO_CHROMA_SCRIPT);
            if (!Files.exists(pythonScript)) {
                LOGGER.log(Level.SEVERE, "Chroma conversion script for Blender not found. Aborting.");
                return BlenderChromaConversionStatus.FAIL;
            }

            Path blendFilePath = path.resolve(blenderFileName);
            final File blendFile = blendFilePath.toFile();

            if (!Files.exists(blendFilePath)) {
                LOGGER.log(Level.SEVERE, "Blender file " + blendFilePath.toString() + " not found. Aborting.");
                return BlenderChromaConversionStatus.FAIL;
            }

            boolean doConversion = false;
            Path meshFile = path.resolve(sceneName + ".mesh.json");
            Path materialFile = path.resolve(sceneName + ".mat.json");
            Path camFile = path.resolve(sceneName + ".cam.json");

            if (Files.notExists(meshFile) || Files.notExists(materialFile) || Files.notExists(meshFile)) {
                doConversion = true;
            } else if (isOutdated(meshFile, blendFile) || isOutdated(materialFile, blendFile) || isOutdated(camFile, blendFile)) {
                // all exist, timestamp outdated?
                doConversion = true;
            }

            if (doConversion) {
                final String cmd = "blender --background " + blendFilePath.toString() + " --python " + pythonScript.toString();
                LOGGER.log(Level.INFO, "Executing shell process " + cmd);
                Process p = Runtime.getRuntime().exec(cmd);
                try {
                    final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    bufferedReader.lines().forEach(System.out::println);
                    p.waitFor();
                    if (p.exitValue() == 0) {
                        return BlenderChromaConversionStatus.OK;
                    } else {
                        LOGGER.log(Level.SEVERE, "Abnormal exit code received from Blender conversion process. Please check sysout!");
                        return BlenderChromaConversionStatus.FAIL;
                    }
                } catch (InterruptedException e) {
                    return BlenderChromaConversionStatus.FAIL;
                }
            } else {
                return BlenderChromaConversionStatus.NOTHING_TODO;
            }
        } catch (InvalidPathException e) {
            LOGGER.log(Level.SEVERE, "Invalid path to for Blender Conversion. Aborting.", e);
            return BlenderChromaConversionStatus.FAIL;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unexpected I/O problem while converting from .blend to chroma format. Aborting.", e);
            return BlenderChromaConversionStatus.FAIL;
        }
    }


    private static boolean isOutdated(Path meshFile, File blendFile) {
        return meshFile.toFile().lastModified() < blendFile.lastModified();
    }


    public static GeometryScene importSceneFromFile(Path path, String blenderFileName) {

        execBlenderToChromaConversion(path, blenderFileName);

        List<Geometry> result;
        List<Material> materials;
        Camera camera;

        boolean jsonFormatMaterials;
        boolean jsonFormatMeshes;

        try {
            String sceneName = blenderFileName.substring(0, blenderFileName.lastIndexOf('.'));
            Path materialFile;
            Path meshFile;

            //prefer json version if existing
            materialFile = path.resolve(sceneName + ".mat.json");
            jsonFormatMaterials = Files.exists(materialFile);
            meshFile = materialFile.resolveSibling(sceneName + ".mesh.json");
            jsonFormatMeshes = Files.exists(meshFile);

            //fallback to binary version
            if (!jsonFormatMaterials) {
                materialFile = path.resolve(sceneName + ".mat.bin");
                if (!Files.exists(materialFile)) {
                    LOGGER.log(Level.SEVERE, "Missing Material file for '{}'. Aborting import.", sceneName);
                    return null;
                }
            }

            if (!jsonFormatMeshes) {
                meshFile = path.resolve(sceneName + ".mesh.bin");
                if (Files.exists(meshFile)) {
                    LOGGER.log(Level.SEVERE, "Missing Mesh file for '{}'. Aborting import.", sceneName);
                    return null;
                }
            }

            materials = jsonFormatMaterials ? importMaterialsFromJson(materialFile) : importMaterialsFromBinary(materialFile);

            result = jsonFormatMeshes ? importMeshesFromJson(meshFile, materials) : importMeshesFromBinary(meshFile, materials);

            camera = importCameraFromJson(path.resolve(sceneName + ".cam.json"));

        } catch (InvalidPathException e) {
            LOGGER.log(Level.SEVERE, "Invalid path to files for Blender Import. Aborting import.", e);
            return null;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unexpected I/O problem while importing. Aborting import.", e);
            return null;
        }

        final GeometryScene geometryScene = new GeometryScene(result);
        geometryScene.setCamera(camera);
        return geometryScene;
    }


    private static Camera importCameraFromJson(Path cameraFile) throws IOException {
        Camera camera;
        final BufferedReader reader = Files.newBufferedReader(cameraFile);

        //Currently only one camera per scene accepted!
        final JsonObject cameraJson = Json.parse(reader).asObject();
        ImmutableVector3 position = toImmVec(cameraJson.get("position").asArray());
        ImmutableVector3 upVector = toImmVec(cameraJson.get("upVector").asArray());
        ImmutableVector3 viewDirection = toImmVec(cameraJson.get("viewDirection").asArray());
        float focalLength = cameraJson.get("focalLength").asFloat() * 1000.0f * (36.0f / 32.0f);// convert m to mm

        ImmutableVector3 col3 = viewDirection.normalize();
        ImmutableVector3 col2 = upVector.normalize();
        ImmutableVector3 col1 = col2.crossProduct(col3).normalize();
        camera = new PinholeCamera(position, new ImmutableMatrix3x3(col1, col2, col3), focalLength, 0.07f, 0.07f, 512, 512);
        return camera;
    }


    private static List<Geometry> importMeshesFromJson(Path meshFile, List<Material> materials) throws IOException {
        List<Geometry> result;
        final BufferedReader reader = Files.newBufferedReader(meshFile);
        final JsonArray jsonArray = Json.parse(reader).asArray();
        result = new ArrayList<>(100);

        for (JsonValue value : jsonArray) {
            JsonObject jsonMesh = value.asObject();
            final String type = jsonMesh.get("type").asString();
            if ("TRIANGULAR_MESH".equals(type)) {
                final JsonArray triangles = jsonMesh.get("triangles").asArray();
                List<Geometry> triangleList = new ArrayList<>(triangles.size());
                for (JsonValue triangleValue : triangles) {
                    JsonObject triangleJson = triangleValue.asObject();
                    final ImmutableVector3 p0 = toImmVec(triangleJson.get("p0").asArray());
                    final ImmutableVector3 p1 = toImmVec(triangleJson.get("p1").asArray());
                    final ImmutableVector3 p2 = toImmVec(triangleJson.get("p2").asArray());
                    final int matIdx = triangleJson.get("m").asInt();
                    triangleList.add(new SimpleTriangle(p0, p1, p2, materials.get(matIdx)));
                }
                result.addAll(triangleList);
            } else {
                LOGGER.log(Level.SEVERE, "Currently only TRIANGULAR_MESH is supported for Geometry. Aborting import.");
                return Collections.emptyList();
            }
        }
        LOGGER.log(Level.INFO, "Successfully imported {0} triangles", result.size());
        return result;
    }


    private static List<Geometry> importMeshesFromBinary(Path meshFile, List<Material> materials) {
        throw new RuntimeException("Not yet implemented");
    }


    private static List<Material> importMaterialsFromBinary(Path materialFile) {
        throw new RuntimeException("Not yet implemented");
    }


    private static List<Material> importMaterialsFromJson(Path materialFile) throws IOException {

        final BufferedReader reader = Files.newBufferedReader(materialFile);
        final JsonArray jsonArray = Json.parse(reader).asArray();
        final List<Material> result = new ArrayList<>(jsonArray.size());

        for (JsonValue value : jsonArray) {
            Material newMaterial;

            JsonObject jsonMaterial = value.asObject();
            String name = jsonMaterial.get("name").asString();

            LOGGER.log(Level.INFO, "Importing JSON Material with name {0}.", name);

            ImmutableVector3 color = toImmVec(jsonMaterial.get("color").asArray());
            float ior = jsonMaterial.get("ior").asFloat();
            float emits = jsonMaterial.get("emits").asFloat();
            float specHardness = jsonMaterial.get("specHardness").asFloat();
            final String typeAsString = jsonMaterial.get("type").asString();
            MaterialType materialType;
            try {
                materialType = MaterialType.valueOf(typeAsString);
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.SEVERE, "Unknown MaterialType {0} found. Aborting import.", typeAsString);
                return Collections.emptyList();
            }

            newMaterial = new Material(materialType, color, ior, emits, specHardness);
            LOGGER.log(Level.INFO, () -> String.format("%s identified as %s.", name, newMaterial.getType()));
            result.add(newMaterial);
        }

        LOGGER.log(Level.INFO, "Successfully imported {0} materials", result.size());

        return result;
    }


    private static ImmutableVector3 toImmVec(JsonArray source) {
        return new ImmutableVector3(source.get(0).asFloat(), source.get(1).asFloat(), source.get(2).asFloat());
    }


    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please define path to Blender file or chroma export set for import.");
        }
        final GeometryScene geometryList = BlenderChromaImporter.importSceneFromBlenderFile(Paths.get(args[0]));
        System.exit(0);
    }


    public static GeometryScene importSceneFromBlenderFile(Path pathToBlendFile) {
        return importSceneFromFile(pathToBlendFile.getParent(), pathToBlendFile.getFileName().toString());
    }
}