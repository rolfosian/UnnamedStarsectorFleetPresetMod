package data.scripts.ui;

import java.util.*;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import data.scripts.ClassRefs;
import data.scripts.util.PresetMiscUtils;
import data.scripts.util.ReflectionUtilis;

@SuppressWarnings("unchecked")
public class TreeTraverser {

    public static class TreeNode {
        private UIPanelAPI parent;
        private List<UIComponentAPI> children;
        private int depth;

        public TreeNode(UIPanelAPI parent, List<UIComponentAPI> children, int depth) {
            this.parent = parent;
            this.children = children;
            this.depth = depth;
        }

        public UIPanelAPI getParent() {
            return this.parent;
        }
        
        public List<UIComponentAPI> getChildren() {
            return this.children;
        }

        public int getDepth() {
            return this.depth;
        }

        public List<ButtonAPI> getButtons() {
            List<ButtonAPI> buttons = new ArrayList<>();

            for (UIComponentAPI child : this.children) {
                if (ButtonAPI.class.isAssignableFrom(child.getClass())) buttons.add((ButtonAPI)child);
            }
            return buttons;
        }

        public List<LabelAPI> getLabels() {
            List<LabelAPI> labels = new ArrayList<>();

            for (UIComponentAPI child : this.children) {
                if (LabelAPI.class.isAssignableFrom(child.getClass())) labels.add((LabelAPI)child);
            }
            return labels;
        }
    }

    private final Object parentPanel;
    private List<TreeNode> nodes;
    private TreeNode targetNode = null;
    private UIComponentAPI targetChild = null;
    private int currentIndex;
    
    // get entire panel tree
    public TreeTraverser(Object parentPanel) {
        this.parentPanel = parentPanel;
        refresh();
    }

    // get panel tree up to depth before limit
    public TreeTraverser(Object parentPanel, int depthLimit) {
        this.parentPanel = parentPanel;
        refresh(depthLimit);
    }

    // beeline to single target child, assuming we know the definite index of the target, final param in varargs should be the index of the target child in the children list
    public TreeTraverser(Object parentPanel, int... treePath) {
        this.parentPanel = parentPanel;
        refresh(treePath);
    }

    public void refresh() {
        this.nodes = new ArrayList<>();
        this.currentIndex = 0;
        this.getChildren((UIComponentAPI)parentPanel, 0);
    }

    public void refresh(int depthLimit) {
        this.nodes = new ArrayList<>();
        this.currentIndex = 0;
        this.getChildren((UIComponentAPI)parentPanel, 0, depthLimit);
    }

    public void refresh(int... treePath) {
        this.nodes = new ArrayList<>();
        this.currentIndex = 0;
        this.getChildren((UIComponentAPI)parentPanel, 0, treePath);
        
        this.targetNode = this.nodes.get(nodes.size()-1);
        this.targetChild = this.targetNode.getChildren().get(treePath[treePath.length-1]);
    }

    private void getChildren(UIComponentAPI parent, int depth) {
        List<UIComponentAPI> children = ClassRefs.uiPanelClass.isInstance(parent) ? (List<UIComponentAPI>) ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelgetChildrenCopyMethod, parent) : null;

        if (children != null && !children.isEmpty()) {
            this.nodes.add(new TreeNode((UIPanelAPI)parent, children, depth));
            depth++;

            for (UIComponentAPI child : children) {
                this.getChildren(child, depth);
            }
        }
        return;
    }

    private void getChildren(UIComponentAPI parent, int depth, int depthLimit) {
        List<UIComponentAPI> children = ClassRefs.uiPanelClass.isInstance(parent) ? (List<UIComponentAPI>) ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelgetChildrenCopyMethod, parent) : null;

        if (children != null && !children.isEmpty()) {
            this.nodes.add(new TreeNode((UIPanelAPI)parent, children, depth));
            depth++;
            if (depth == depthLimit) return;

            for (UIComponentAPI child : children) {
                this.getChildren(child, depth, depthLimit);
            }
        }
        return;
    }

    private void getChildren(UIComponentAPI parent, int depth, int... treePath) {
        List<UIComponentAPI> children = ClassRefs.uiPanelClass.isInstance(parent) ? (List<UIComponentAPI>) ReflectionUtilis.invokeMethodDirectly(ClassRefs.uiPanelgetChildrenCopyMethod, parent) : null;

        if (children != null && !children.isEmpty()) {
            this.nodes.add(new TreeNode((UIPanelAPI)parent, children, depth));
            depth++;
            if (depth == treePath.length) return;

            this.getChildren(children.get(treePath[depth-1]), depth, treePath);
        }
        return;
    }

    public List<Integer> getPathToTarget(UIComponentAPI target) {
        List<Integer> result = new ArrayList<>();
        UIComponentAPI current = target;

        while (!(current == this.parentPanel)) {
            outer:
            for (int i = this.nodes.size()-1; i >= 0; i--) {
                TreeNode node = nodes.get(i);

                for (int j = 0; j < node.getChildren().size(); j++) {
                    if (node.getChildren().get(j) == current) {
                        current = node.getParent();
                        result.add(0, j);
                        break outer;
                    }
                }
            }
        }

        return result;
    }

    public void logPathToTarget(UIComponentAPI target) {
        Global.getLogger(getClass()).info("Path to target: " + String.valueOf(getPathToTarget(target)));
    }

    public void removeBranch() {
        TreeNode node = getCurrentNode();
        UIPanelAPI parent = node.getParent();
        if (parent == null) return;

        for (UIComponentAPI child : node.getChildren()) {
            parent.removeComponent(child);
        }
        refresh();
    }

    public UIComponentAPI getTargetChild() {
        return this.targetChild;
    }

    public TreeNode getTargetNode() {
        return this.targetNode;
    }

    public List<TreeNode> getNodes() {
        return this.nodes;
    }

    public TreeNode getCurrentNode() {
        return nodes.get(currentIndex);
    }

    public TreeNode getNode(int index) {
        return this.nodes.get(index);
    }

    public List<TreeNode> getNodesAtDepth(int depth) {
        List<TreeNode> result = new ArrayList<>(); 
        for (TreeNode node : this.nodes) if (node.getDepth() == depth) result.add(node);
        return result;
    }
    
    public boolean goUpOneLevel() {
        if (currentIndex == 0) return false;
        currentIndex -= 1;
        return true;
    }
    
    public boolean goDownOneLevel() {
        if (currentIndex >= nodes.size() - 1) return false;
        currentIndex += 1;
        return true;
    }

    public void toTop() {
        currentIndex = nodes.size() - 1;
    }
    
    public void toBottom() {
        currentIndex = 0;
    }

    public void clearPanel() {
        while (this.goDownOneLevel()) {}
        for (int i = 0; i < this.getNodes().size(); i++) {
            UIPanelAPI parent = this.getCurrentNode().getParent();

            for (UIComponentAPI child : this.getCurrentNode().getChildren()) {
                parent.removeComponent(child);
            }
            this.goUpOneLevel();
        }
    }
}