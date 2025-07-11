package data.scripts.ui;

import java.util.*;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.LabelAPI;

import data.scripts.util.ReflectionUtilis;

public class TreeTraverser {

    public static class TreeNode {
        private Object parent;
        private List<Object> children;
        private int depth;

        public TreeNode(Object parent, List<Object> children, int depth) {
            this.parent = parent;
            this.children = children;
            this.depth = depth;
        }

        public Object getParent() {
            return this.parent;
        }
        
        public List<Object> getChildren() {
            return this.children;
        }

        public int getDepth() {
            return this.depth;
        }

        public List<ButtonAPI> getButtons() {
            List<ButtonAPI> buttons = new ArrayList<>();

            for (Object child : this.children) {
                if (ButtonAPI.class.isAssignableFrom(child.getClass())) buttons.add((ButtonAPI)child);
            }
            return buttons;
        }

        public List<LabelAPI> getLabels() {
            List<LabelAPI> labels = new ArrayList<>();

            for (Object child : this.children) {
                if (LabelAPI.class.isAssignableFrom(child.getClass())) labels.add((LabelAPI)child);
            }
            return !labels.isEmpty() ? labels : null;
        }
    }

    private final Object parentPanel;
    private List<TreeNode> nodes;
    private int currentIndex;

    public TreeTraverser(Object parentPanel) {
        this.parentPanel = parentPanel;
        refresh();
    }

    public void removeBranch() {
        TreeNode node = getCurrentNode();
        Object parent = node.getParent();
        if (parent == null) return;

        UIPanel pp =  new UIPanel(parent);
        for (Object child : node.getChildren()) {
            pp.remove(new UIComponent(child));
        }
        refresh();
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

    public void refresh() {
        this.nodes = new ArrayList<>();
        this.currentIndex = 0;
        this.getChildren(parentPanel, null, 0);
    }

    public void clearPanel() { // this is dangerous to use and i cant be bothered debugging it, sometimes it goes above the top level for some reason idk. it's good for clearing ConfirmDialog panels though
        while (this.goDownOneLevel()) {}
        for (int i = 0; i < this.getNodes().size(); i++) {
            UIPanel par = new UIPanel(this.getCurrentNode().getParent());
            for (Object child : this.getCurrentNode().getChildren()) {
                par.remove(new UIComponent(child));
            }
            this.goUpOneLevel();
        }
    }
    
    @SuppressWarnings("unchecked")
    private void getChildren(Object node, Object parent, int depth) {
        List<Object> children = (List<Object>) ReflectionUtilis.getMethodAndInvokeDirectly("getChildrenCopy", node, 0);

        if (children != null && !children.isEmpty()) {
            if (parent == null) {
                this.nodes.add(new TreeNode(node, children, depth));
            } else {
                this.nodes.add(new TreeNode(parent, children, depth));
            }
            depth++;

            for (Object child : children) {
                this.getChildren(child, node, depth);
            }
        }
        return;
    }
}