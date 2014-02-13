package net.sourceforge.fenixedu.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sourceforge.fenixedu.domain.exception.SpaceDomainException;
import net.sourceforge.fenixedu.domain.space.Campus;
import net.sourceforge.fenixedu.util.DomainObjectUtil;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.joda.time.DateTime;

import pt.utl.ist.fenix.tools.util.i18n.MultiLanguageString;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

public class PunctualRoomsOccupationRequest extends PunctualRoomsOccupationRequest_Base {

    public static final Comparator<PunctualRoomsOccupationRequest> COMPARATOR_BY_IDENTIFICATION = new BeanComparator(
            "identification");
    public static final Comparator<PunctualRoomsOccupationRequest> COMPARATOR_BY_INSTANT = new ComparatorChain();
    public static final Comparator<PunctualRoomsOccupationRequest> COMPARATOR_BY_MORE_RECENT_COMMENT_INSTANT =
            new ComparatorChain();
    static {
        ((ComparatorChain) COMPARATOR_BY_MORE_RECENT_COMMENT_INSTANT).addComparator(
                new BeanComparator("moreRecentCommentInstant"), true);
        ((ComparatorChain) COMPARATOR_BY_MORE_RECENT_COMMENT_INSTANT).addComparator(DomainObjectUtil.COMPARATOR_BY_ID);

        ((ComparatorChain) COMPARATOR_BY_INSTANT).addComparator(new BeanComparator("instant"), true);
        ((ComparatorChain) COMPARATOR_BY_INSTANT).addComparator(DomainObjectUtil.COMPARATOR_BY_ID);
    }

    public PunctualRoomsOccupationRequest(User requestor, MultiLanguageString subject, Campus campus,
            MultiLanguageString description) {
//        check(this, ResourceAllocationRolePredicates.checkPermissionsToManagePunctualRoomsOccupationRequests);
        super();
        checkIfRequestAlreadyExists(requestor, subject, description);
        setRootDomainObject(Bennu.getInstance());
        setRequestor(requestor);
        DateTime now = new DateTime();
        setInstant(now);
        setCampus(campus);
        addStateInstants(new PunctualRoomsOccupationStateInstant(this, RequestState.NEW, now));
        addComments(new PunctualRoomsOccupationComment(this, subject, description, requestor, now));
        setTeacherReadComments(1);
        setEmployeeReadComments(0);
        setIdentification(getNextRequestIdentification());
    }

    @jvstm.cps.ConsistencyPredicate
    protected boolean checkRequiredParameters() {
        return getInstant() != null && getIdentification() != null;
    }

    public Integer getNumberOfNewComments(User person) {
        if (person.equals(getOwner())) {
            return getCommentsSet().size() - getEmployeeReadComments();
        } else if (person.equals(getRequestor())) {
            return getCommentsSet().size() - getTeacherReadComments();
        }
        return Integer.valueOf(0);
    }

    public DateTime getMoreRecentCommentInstant() {
        SortedSet<PunctualRoomsOccupationComment> result =
                new TreeSet<PunctualRoomsOccupationComment>(PunctualRoomsOccupationComment.COMPARATOR_BY_INSTANT);
        result.addAll(getComments());
        return result.last().getInstant();
    }

    public void associateNewGenericEvent(User person, GenericEvent event, DateTime instant) {
        if (getCurrentState().equals(RequestState.RESOLVED)) {
            throw new SpaceDomainException("error.PunctualRoomsOccupationRequest.impossible.associate.new.genericEvent");
        }
        addGenericEvents(event);
        openRequestWithoutAssociateOwner(instant);
        setOwner(person);
    }

    public void createNewTeacherOrEmployeeComment(MultiLanguageString description, User commentOwner, DateTime instant) {
        new PunctualRoomsOccupationComment(this, getCommentSubject(), description, commentOwner, instant);
        if (commentOwner.equals(getRequestor())) {
            setTeacherReadComments(getCommentsSet().size());
        } else {
            setOwner(commentOwner);
            setEmployeeReadComments(getCommentsSet().size());
        }
    }

    public void createNewTeacherCommentAndOpenRequest(MultiLanguageString description, User commentOwner, DateTime instant) {
        openRequestWithoutAssociateOwner(instant);
        new PunctualRoomsOccupationComment(this, getCommentSubject(), description, commentOwner, instant);
        setTeacherReadComments(getCommentsSet().size());
    }

    public void createNewEmployeeCommentAndCloseRequest(MultiLanguageString description, User commentOwner, DateTime instant) {
        new PunctualRoomsOccupationComment(this, getCommentSubject(), description, commentOwner, instant);
        closeRequestWithoutAssociateOwner(instant);
        setOwner(commentOwner);
        setEmployeeReadComments(getCommentsSet().size());
    }

    public void closeRequestAndAssociateOwnerOnlyForEmployees(DateTime instant, User person) {
        closeRequestWithoutAssociateOwner(instant);
        if (!getOwner().equals(person)) {
            setEmployeeReadComments(0);
            setOwner(person);
        }
    }

    public void openRequestAndAssociateOwnerOnlyForEmployess(DateTime instant, User person) {
        openRequestWithoutAssociateOwner(instant);
        if (getOwner() == null || !getOwner().equals(person)) {
            setEmployeeReadComments(0);
            setOwner(person);
        }
    }

    private void closeRequestWithoutAssociateOwner(DateTime instant) {
        if (!getCurrentState().equals(RequestState.RESOLVED)) {
            addStateInstants(new PunctualRoomsOccupationStateInstant(this, RequestState.RESOLVED, instant));
        }
    }

    private void openRequestWithoutAssociateOwner(DateTime instant) {
        if (!getCurrentState().equals(RequestState.OPEN)) {
            addStateInstants(new PunctualRoomsOccupationStateInstant(this, RequestState.OPEN, instant));
        }
    }

    private MultiLanguageString getCommentSubject() {
        StringBuilder subject = new StringBuilder();
        subject.append("Re: ");
        PunctualRoomsOccupationComment firstComment = getFirstComment();
        if (firstComment != null) {
            subject.append(firstComment.getSubject().getContent());
        }
        return new MultiLanguageString(subject.toString());
    }

    @Override
    public void setOwner(User owner) {
        if (owner == null || !owner.equals(getRequestor())) {
            super.setOwner(owner);
        }
    }

    @Override
    public void setIdentification(Integer identification) {
        if (identification == null) {
            throw new SpaceDomainException("error.PunctualRoomsOccupationRequest.empty.identification");
        }
        super.setIdentification(identification);
    }

    @Override
    public void setRequestor(User requestor) {
        if (requestor == null) {
            throw new SpaceDomainException("error.PunctualRoomsOccupationRequest.empty.requestor");
        }
        super.setRequestor(requestor);
    }

    @Override
    public void setInstant(DateTime instant) {
        if (instant == null) {
            throw new SpaceDomainException("error.PunctualRoomsOccupationRequest.empty.instant");
        }
        super.setInstant(instant);
    }

    public String getPresentationInstant() {
        return getInstant().toString("dd/MM/yyyy HH:mm");
    }

    public static Set<PunctualRoomsOccupationRequest> getRequestsByTypeOrderByDate(RequestState state, Campus campus) {
        Set<PunctualRoomsOccupationRequest> result =
                new TreeSet<PunctualRoomsOccupationRequest>(PunctualRoomsOccupationRequest.COMPARATOR_BY_INSTANT);
        for (PunctualRoomsOccupationRequest request : Bennu.getInstance().getPunctualRoomsOccupationRequestsSet()) {
            if (request.getCurrentState().equals(state) && (request.getCampus() == null || request.getCampus().equals(campus))) {
                result.add(request);
            }
        }
        return result;
    }

    public static PunctualRoomsOccupationRequest getRequestById(Integer requestID) {
        for (PunctualRoomsOccupationRequest request : Bennu.getInstance().getPunctualRoomsOccupationRequestsSet()) {
            if (request.getIdentification().equals(requestID)) {
                return request;
            }
        }
        return null;
    }

    public static Set<PunctualRoomsOccupationRequest> getResolvedRequestsOrderByMoreRecentComment(Campus campus) {
        Set<PunctualRoomsOccupationRequest> result =
                new TreeSet<PunctualRoomsOccupationRequest>(
                        PunctualRoomsOccupationRequest.COMPARATOR_BY_MORE_RECENT_COMMENT_INSTANT);
        for (PunctualRoomsOccupationRequest request : Bennu.getInstance().getPunctualRoomsOccupationRequestsSet()) {
            if (request.getCurrentState().equals(RequestState.RESOLVED)
                    && (request.getCampus() == null || request.getCampus().equals(campus))) {
                result.add(request);
            }
        }
        return result;
    }

    public static Set<PunctualRoomsOccupationRequest> getRequestsByTypeAndDiferentOwnerOrderByDate(RequestState state,
            User owner, Campus campus) {
        Set<PunctualRoomsOccupationRequest> result =
                new TreeSet<PunctualRoomsOccupationRequest>(PunctualRoomsOccupationRequest.COMPARATOR_BY_INSTANT);
        for (PunctualRoomsOccupationRequest request : Bennu.getInstance().getPunctualRoomsOccupationRequestsSet()) {
            if (request.getCurrentState().equals(state) && (request.getOwner() == null || !request.getOwner().equals(owner))
                    && (request.getCampus() == null || request.getCampus().equals(campus))) {
                result.add(request);
            }
        }
        return result;
    }

    public PunctualRoomsOccupationComment getFirstComment() {
        for (PunctualRoomsOccupationComment comment : getComments()) {
            if (comment.getInstant().isEqual(getInstant())) {
                return comment;
            }
        }
        return null;
    }

    public Set<PunctualRoomsOccupationComment> getCommentsWithoutFirstCommentOrderByDate() {
        Set<PunctualRoomsOccupationComment> result =
                new TreeSet<PunctualRoomsOccupationComment>(PunctualRoomsOccupationComment.COMPARATOR_BY_INSTANT);
        for (PunctualRoomsOccupationComment comment : getComments()) {
            if (!comment.getInstant().isEqual(getInstant())) {
                result.add(comment);
            }
        }
        return result;
    }

    public String getSubject() {
        final PunctualRoomsOccupationComment firstComment = getFirstComment();
        final String content = firstComment != null ? firstComment.getSubject().getContent() : null;
        return content == null || content.isEmpty() ? getIdentification().toString() : content;
    }

    public String getDescription() {
        final PunctualRoomsOccupationComment firstComment = getFirstComment();
        final MultiLanguageString description = firstComment == null ? null : firstComment.getDescription();
        final String content = description == null ? null : description.getContent();
        return content == null ? getExternalId() : content;
    }

    public RequestState getCurrentState() {
        SortedSet<PunctualRoomsOccupationStateInstant> result =
                new TreeSet<PunctualRoomsOccupationStateInstant>(PunctualRoomsOccupationStateInstant.COMPARATOR_BY_INSTANT);

        result.addAll(getStateInstants());
        return result.last().getRequestState();
    }

    public RequestState getState(DateTime instanTime) {
        if (instanTime == null) {
            return getCurrentState();
        } else {
            for (PunctualRoomsOccupationStateInstant stateInstant : getStateInstants()) {
                if (stateInstant.getInstant().isEqual(instanTime)) {
                    return stateInstant.getRequestState();
                }
            }
        }
        return null;
    }

    private Integer getNextRequestIdentification() {
        SortedSet<PunctualRoomsOccupationRequest> result =
                new TreeSet<PunctualRoomsOccupationRequest>(PunctualRoomsOccupationRequest.COMPARATOR_BY_IDENTIFICATION);
        Collection<PunctualRoomsOccupationRequest> requests = Bennu.getInstance().getPunctualRoomsOccupationRequestsSet();
        for (PunctualRoomsOccupationRequest request : requests) {
            if (!request.equals(this)) {
                result.add(request);
            }
        }
        return result.isEmpty() ? 1 : result.last().getIdentification() + 1;
    }

    public Set<GenericEvent> getActiveGenericEvents() {

        return FluentIterable.from(getGenericEventsSet()).filter(new Predicate<GenericEvent>() {

            @Override
            public boolean apply(GenericEvent input) {
                return input.isActive();
            }
        }).toSet();

    }

    private void checkIfRequestAlreadyExists(User requestor, MultiLanguageString subject, MultiLanguageString description) {
        Set<PunctualRoomsOccupationRequest> requests = requestor.getPunctualRoomsOccupationRequestsSet();
        for (PunctualRoomsOccupationRequest request : requests) {
            PunctualRoomsOccupationComment firstComment = request.getFirstComment();
            if (firstComment != null && firstComment.getSubject() != null && firstComment.getSubject().compareTo(subject) == 0
                    && firstComment.getDescription() != null && firstComment.getDescription().compareTo(description) == 0) {
                throw new SpaceDomainException("error.PunctualRoomsOccupationRequest.request.already.exists");
            }
        }
    }

    @Deprecated
    public java.util.Set<net.sourceforge.fenixedu.domain.PunctualRoomsOccupationStateInstant> getStateInstants() {
        return getStateInstantsSet();
    }

    @Deprecated
    public boolean hasAnyStateInstants() {
        return !getStateInstantsSet().isEmpty();
    }

    @Deprecated
    public java.util.Set<net.sourceforge.fenixedu.domain.GenericEvent> getGenericEvents() {
        return getGenericEventsSet();
    }

    @Deprecated
    public boolean hasAnyGenericEvents() {
        return !getGenericEventsSet().isEmpty();
    }

    @Deprecated
    public java.util.Set<net.sourceforge.fenixedu.domain.PunctualRoomsOccupationComment> getComments() {
        return getCommentsSet();
    }

    @Deprecated
    public boolean hasAnyComments() {
        return !getCommentsSet().isEmpty();
    }

    @Deprecated
    public boolean hasOwner() {
        return getOwner() != null;
    }

    @Deprecated
    public boolean hasBennu() {
        return getRootDomainObject() != null;
    }

    @Deprecated
    public boolean hasInstant() {
        return getInstant() != null;
    }

    @Deprecated
    public boolean hasEmployeeReadComments() {
        return getEmployeeReadComments() != null;
    }

    @Deprecated
    public boolean hasIdentification() {
        return getIdentification() != null;
    }

    @Deprecated
    public boolean hasRequestor() {
        return getRequestor() != null;
    }

    @Deprecated
    public boolean hasTeacherReadComments() {
        return getTeacherReadComments() != null;
    }

}