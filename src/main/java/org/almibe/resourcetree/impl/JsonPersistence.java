package org.almibe.resourcetree.impl;

import com.google.gson.Gson;
import org.almibe.resourcetree.ResourceTree;
import org.almibe.resourcetree.ResourceTreePersistence;
import org.almibe.resourcetree.TreeModel;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.*;

public class JsonPersistence<T> implements ResourceTreePersistence<T> {
    private final Gson gson = new Gson();
    private final File jsonFile;
    private final ResourceTree<T> resourceTree;
    private final Type type;

    public JsonPersistence(File jsonFile, ResourceTree<T> resourceTree, Type type) {
        this.jsonFile = jsonFile;
        this.resourceTree = resourceTree;
        this.type = type;
    }

    @Override
    public void move(T node, T parent) {
        writeJsonFile();
    }

    @Override
    public void add(T node) {
        writeJsonFile();
    }

    @Override
    public void add(T node, T parent) {
        writeJsonFile();
    }

    @Override
    public void remove(T node) {
        writeJsonFile();
    }

    @Override
    public void update(T node) {
        writeJsonFile();
    }

    @Override
    public void load() {
        if (!jsonFile.exists()) {
            return;
        }
        try (Reader reader = new FileReader(jsonFile)) {
            resourceTree.setTreePersistence(new NullPersistence()); //start with null persistence while loading
            List<TreeModel<T>> rootModels = gson.fromJson(reader, type);
            reader.close();

            Map<T, List<TreeModel<T>>> parentMap = new HashMap<>();
            Map<T, List<TreeModel<T>>> nextParentMap = new HashMap<>();

            for (TreeModel<T> treeModel : rootModels) {
                resourceTree.add(treeModel.getNode());
                parentMap.put(treeModel.getNode(), treeModel.getChildren());
            }

            while (listOfListSize(parentMap.values()) > 0) {
                nextParentMap.clear();
                for (T parent : parentMap.keySet()) {
                    for (TreeModel<T> treeModel : parentMap.get(parent)) {
                        nextParentMap.put(treeModel.getNode(), treeModel.getChildren());
                        resourceTree.add(treeModel.getNode(), parent);
                    }
                }
                parentMap.clear();
                parentMap.putAll(nextParentMap);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            resourceTree.setTreePersistence(this);
        }
    }

    @Override
    public void clear() {
        writeJsonFile();
    }

    private void writeJsonFile() {
        List<TreeModel<T>> modelList = new ArrayList<>();
        List<TreeModel<T>> parents = new ArrayList<>();
        List<TreeModel<T>> nextParents = new ArrayList<>();
        List<List<T>> children = new ArrayList<>();
        List<List<T>> nextChildren = new ArrayList<>();
        for (T t : resourceTree.getRootItems()) {
            modelList.add(new TreeModel<>(t));
            children.add(resourceTree.getChildren(t));
        }
        parents.addAll(modelList);
        while (listOfListSize(children) > 0) {
            nextChildren.clear();
            nextParents.clear();
            for (int i = 0; i < children.size(); i++) {
                for (T t : children.get(i)) {
                    nextChildren.add(resourceTree.getChildren(t));
                    parents.get(i).getChildren().add(new TreeModel<>(t));
                }
                nextParents.addAll(parents.get(i).getChildren());
            }
            parents.clear();
            parents.addAll(nextParents);
            children.clear();
            children.addAll(nextChildren);
        }

        try (FileWriter fileWriter = new FileWriter(jsonFile)) {
            fileWriter.write(gson.toJson(modelList));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private <L> int listOfListSize(Collection<List<L>> lists) {
        int size = 0;
        for (List<L> list : lists) {
            size += list.size();
        }
        return size;
    }
}
